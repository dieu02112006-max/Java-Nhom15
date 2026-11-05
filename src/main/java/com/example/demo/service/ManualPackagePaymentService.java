package com.example.demo.service;

import com.example.demo.dto.response.PackageCheckoutResponse;
import com.example.demo.dto.response.PackagePaymentStatusResponse;
import com.example.demo.dto.response.PackagePlanResponse;
import com.example.demo.dto.response.PackageReviewResponse;
import com.example.demo.exception.BusinessException;
import com.example.demo.exception.ResourceNotFoundException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class ManualPackagePaymentService {
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String QR_IMAGE_URL = "/images/package-payment-qr.png";

    private final JdbcTemplate jdbc;
    private final ObjectProvider<JavaMailSender> mailSenders;
    private final String mailHost;
    private final String mailUsername;
    private final String mailPassword;
    private final String fromAddress;
    private final String reviewerEmail;
    private final String reviewPageUrl;

    public ManualPackagePaymentService(
            JdbcTemplate jdbc,
            ObjectProvider<JavaMailSender> mailSenders,
            @Value("${spring.mail.host:}") String mailHost,
            @Value("${spring.mail.username:}") String mailUsername,
            @Value("${spring.mail.password:}") String mailPassword,
            @Value("${app.mail.from:}") String fromAddress,
            @Value("${app.package-payment.review-email:dieu02112006@gmail.com}") String reviewerEmail,
            @Value("${app.package-payment.review-page-url:http://localhost:8080/payment-review.html}") String reviewPageUrl) {
        this.jdbc = jdbc;
        this.mailSenders = mailSenders;
        this.mailHost = mailHost;
        this.mailUsername = mailUsername;
        this.mailPassword = mailPassword;
        this.fromAddress = fromAddress;
        this.reviewerEmail = reviewerEmail;
        this.reviewPageUrl = reviewPageUrl;
    }

    public boolean isEnabled() {
        return StringUtils.hasText(mailHost) && StringUtils.hasText(mailUsername)
                && StringUtils.hasText(mailPassword) && StringUtils.hasText(fromAddress) && StringUtils.hasText(reviewerEmail)
                && mailSenders.getIfAvailable() != null;
    }

    @Transactional
    public PackageCheckoutResponse createCheckout(Long userId, PackagePlanResponse plan, String userName, String userEmail) {
        if (plan.priceVnd() <= 0) throw new BusinessException("Gói miễn phí không cần thanh toán");

        String transactionRef = java.util.UUID.randomUUID().toString().replace("-", "");
        byte[] tokenBytes = new byte[32];
        RANDOM.nextBytes(tokenBytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
        String tokenHash = sha256(token);
        LocalDateTime now = LocalDateTime.now();
        jdbc.update("""
                INSERT INTO package_payments
                    (txn_ref, user_id, plan_code, amount_vnd, status, review_token_hash, review_expires_at, created_at)
                VALUES (?, ?, ?, ?, 'PENDING', ?, ?, ?)
                """, transactionRef, userId, plan.code(), plan.priceVnd(), tokenHash, now.plusHours(24), now);

        boolean emailSent = false;
        if (isEnabled()) {
            try {
                sendReviewEmail(transactionRef, plan, userName, userEmail, token);
                emailSent = true;
            } catch (MailException exception) {
                org.slf4j.LoggerFactory.getLogger(ManualPackagePaymentService.class)
                        .warn("Could not send package payment review email for order {}", transactionRef, exception);
            }
        }
        return new PackageCheckoutResponse(transactionRef, plan.name(), plan.priceVnd(), QR_IMAGE_URL, "PENDING", emailSent);
    }

    @Transactional
    public PackagePaymentStatusResponse getStatus(Long userId, String transactionRef) {
        java.util.List<StatusRow> rows = jdbc.query("""
                SELECT txn_ref, plan_code, amount_vnd, status, review_expires_at
                FROM package_payments WHERE txn_ref = ? AND user_id = ?
                """, (rs, row) -> new StatusRow(rs.getString("txn_ref"), rs.getString("plan_code"),
                rs.getLong("amount_vnd"), rs.getString("status"),
                rs.getObject("review_expires_at", LocalDateTime.class)), transactionRef, userId);
        if (rows.isEmpty()) throw new ResourceNotFoundException("Package payment", transactionRef);
        StatusRow row = rows.getFirst();
        String status = row.status();
        if ("PENDING".equals(status) && (row.expiresAt() == null || row.expiresAt().isBefore(LocalDateTime.now()))) {
            jdbc.update("UPDATE package_payments SET status='FAILED', review_token_hash=NULL WHERE txn_ref=?", transactionRef);
            status = "FAILED";
        }
        return new PackagePaymentStatusResponse(row.transactionRef(), row.planCode(), row.amountVnd(), status);
    }

    @Transactional(readOnly = true)
    public PackageReviewResponse getReview(String token) {
        String hash = sha256(token);
        return jdbc.query("""
                SELECT p.txn_ref, p.plan_code, p.amount_vnd, p.status, u.full_name, p.review_expires_at
                FROM package_payments p JOIN users u ON u.id = p.user_id
                WHERE p.review_token_hash = ?
                """, rs -> {
                    if (!rs.next()) throw new BusinessException("Liên kết xác nhận không hợp lệ hoặc đã được sử dụng");
                    if (!"PENDING".equals(rs.getString("status"))) {
                        throw new BusinessException("Giao dịch này đã được xử lý");
                    }
                    LocalDateTime expiresAt = rs.getObject("review_expires_at", LocalDateTime.class);
                    if (expiresAt == null || expiresAt.isBefore(LocalDateTime.now())) {
                        throw new BusinessException("Liên kết xác nhận đã hết hạn");
                    }
                    return new PackageReviewResponse(rs.getString("txn_ref"), rs.getString("plan_code"),
                            rs.getLong("amount_vnd"), rs.getString("status"), rs.getString("full_name"));
                }, hash);
    }

    @Transactional
    public PackagePaymentStatusResponse review(String token, boolean paid) {
        String hash = sha256(token);
        java.util.List<ReviewOrder> rows = jdbc.query("""
                SELECT txn_ref, user_id, plan_code, amount_vnd, status, review_expires_at
                FROM package_payments p WHERE p.review_token_hash = ? FOR UPDATE
                """, (rs, row) -> new ReviewOrder(rs.getString("txn_ref"), rs.getLong("user_id"),
                rs.getString("plan_code"), rs.getLong("amount_vnd"), rs.getString("status"),
                rs.getObject("review_expires_at", LocalDateTime.class)), hash);
        if (rows.isEmpty()) throw new BusinessException("Liên kết xác nhận không hợp lệ hoặc đã được sử dụng");
        ReviewOrder order = rows.getFirst();
        if (!"PENDING".equals(order.status())) throw new BusinessException("Giao dịch này đã được xử lý");
        if (order.expiresAt() == null || order.expiresAt().isBefore(LocalDateTime.now())) {
            jdbc.update("UPDATE package_payments SET status='FAILED', review_token_hash=NULL WHERE txn_ref=?", order.transactionRef());
            return new PackagePaymentStatusResponse(order.transactionRef(), order.planCode(), order.amountVnd(), "FAILED");
        }
        String finalStatus = paid ? "PAID" : "FAILED";
        int changed = jdbc.update("""
                UPDATE package_payments SET status = ?, paid_at = ?, review_token_hash = NULL
                WHERE txn_ref = ? AND status = 'PENDING'
                """, finalStatus, paid ? LocalDateTime.now() : null, order.transactionRef());
        if (changed != 1) throw new BusinessException("Giao dịch này đã được xử lý");
        if (paid) jdbc.update("UPDATE users SET subscription_tier = ? WHERE id = ?", order.planCode(), order.userId());
        return new PackagePaymentStatusResponse(order.transactionRef(), order.planCode(), order.amountVnd(), finalStatus);
    }

    private void sendReviewEmail(String transactionRef, PackagePlanResponse plan,
                                 String userName, String userEmail, String token) {
        JavaMailSender sender = mailSenders.getIfAvailable();
        if (sender == null) throw new BusinessException("Không thể kết nối SMTP để gửi email xác nhận.");
        String reviewUrl = reviewPageUrl + (reviewPageUrl.contains("?") ? "&" : "?") + "token=" + token;
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(reviewerEmail);
        message.setSubject("Xác nhận thanh toán gói Dayflow " + plan.name());
        message.setText("Có yêu cầu xác nhận thanh toán gói Dayflow.\n\n"
                + "Tài khoản: " + userName + " (" + userEmail + ")\n"
                + "Gói: " + plan.name() + " (" + plan.code() + ")\n"
                + "Số tiền cần nhận: " + String.format("%,d", plan.priceVnd()) + " VND\n"
                + "Mã đơn: " + transactionRef + "\n\n"
                + "Nội dung chuyển khoản nên có: DAYFLOW " + transactionRef + "\n\n"
                + "Hãy kiểm tra tài khoản ngân hàng trước khi xác nhận thanh toán.\n"
                + "Mở trang xác nhận để chọn thành công hoặc thất bại:\n" + reviewUrl + "\n\n"
                + "Liên kết chỉ dùng một lần và hết hạn sau 24 giờ.");
        sender.send(message);
    }

    private String sha256(String value) {
        if (!StringUtils.hasText(value)) throw new BusinessException("Thiếu mã xác nhận thanh toán");
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("Không thể tạo mã xác nhận thanh toán", exception);
        }
    }

    private record StatusRow(String transactionRef, String planCode, long amountVnd,
                             String status, LocalDateTime expiresAt) {}
    private record ReviewOrder(String transactionRef, Long userId, String planCode, long amountVnd,
                               String status, LocalDateTime expiresAt) {}
}
