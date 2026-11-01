package com.example.demo.service;

import java.sql.Timestamp;
import java.time.DateTimeException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class TaskEmailReminderService {
    private static final Logger log = LoggerFactory.getLogger(TaskEmailReminderService.class);
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final JdbcTemplate jdbc;
    private final ObjectProvider<JavaMailSender> mailSenders;
    private final String mailHost;
    private final String fromAddress;
    private final AtomicBoolean missingMailConfigLogged = new AtomicBoolean();

    public TaskEmailReminderService(
            JdbcTemplate jdbc,
            ObjectProvider<JavaMailSender> mailSenders,
            @Value("${spring.mail.host:}") String mailHost,
            @Value("${app.mail.from:}") String fromAddress) {
        this.jdbc = jdbc;
        this.mailSenders = mailSenders;
        this.mailHost = mailHost;
        this.fromAddress = fromAddress;
    }

    @Scheduled(
            initialDelayString = "${app.task-reminder.initial-delay-ms:30000}",
            fixedDelayString = "${app.task-reminder.interval-ms:30000}")
    public void sendUpcomingTaskReminders() {
        if (!StringUtils.hasText(mailHost)) {
            if (missingMailConfigLogged.compareAndSet(false, true)) {
                log.info("Task email reminders are disabled until MAIL_HOST and SMTP credentials are configured.");
            }
            return;
        }
        JavaMailSender sender = mailSenders.getIfAvailable();
        if (sender == null) return;

        Instant now = Instant.now();
        LocalDateTime serverNow = LocalDateTime.now();
        List<TaskReminder> tasks = jdbc.query("""
                SELECT t.id, t.title, t.due_date, u.email, u.full_name, u.timezone
                FROM tasks t
                JOIN users u ON u.id = t.user_id
                WHERE t.status <> 'done'
                  AND t.due_date IS NOT NULL
                  AND t.due_date BETWEEN ? AND ?
                  AND u.is_active = 1
                  AND NOT EXISTS (
                      SELECT 1 FROM task_email_reminders r
                      WHERE r.task_id = t.id AND r.due_date = t.due_date
                  )
                """, (rs, rowNum) -> new TaskReminder(
                rs.getLong("id"), rs.getString("title"), rs.getTimestamp("due_date").toLocalDateTime(),
                rs.getString("email"), rs.getString("full_name"), rs.getString("timezone")),
                Timestamp.valueOf(serverNow.minusHours(30)), Timestamp.valueOf(serverNow.plusHours(40)));

        for (TaskReminder task : tasks) {
            ZoneId zone = zoneFor(task.timezone());
            Instant dueAt = task.dueDate().atZone(zone).toInstant();
            Instant reminderAt = dueAt.minus(Duration.ofHours(1));
            if (now.isBefore(reminderAt) || !now.isBefore(dueAt)) continue;
            if (!claim(task, serverNow)) continue;
            try {
                send(sender, task, zone, dueAt, now);
            } catch (RuntimeException exception) {
                jdbc.update("""
                        UPDATE task_email_reminders SET status = 'FAILED'
                        WHERE task_id = ? AND due_date = ? AND status = 'PROCESSING'
                        """, task.id(), Timestamp.valueOf(task.dueDate()));
                log.warn("Could not send task reminder for task {} to {}", task.id(), task.email(), exception);
                continue;
            }
            jdbc.update("""
                    UPDATE task_email_reminders SET status = 'SENT', sent_at = ?
                    WHERE task_id = ? AND due_date = ?
                    """, Timestamp.valueOf(LocalDateTime.now()), task.id(), Timestamp.valueOf(task.dueDate()));
        }
    }

    private boolean claim(TaskReminder task, LocalDateTime now) {
        Timestamp dueDate = Timestamp.valueOf(task.dueDate());
        Timestamp claimedAt = Timestamp.valueOf(now);
        try {
            jdbc.update("""
                    INSERT INTO task_email_reminders (task_id, due_date, status, claimed_at)
                    VALUES (?, ?, 'PROCESSING', ?)
                    """, task.id(), dueDate, claimedAt);
            return true;
        } catch (DuplicateKeyException duplicate) {
            return false;
        }
    }

    private void send(JavaMailSender sender, TaskReminder task, ZoneId zone, Instant dueAt, Instant now) {
        SimpleMailMessage message = new SimpleMailMessage();
        if (StringUtils.hasText(fromAddress)) message.setFrom(fromAddress);
        message.setTo(task.email());
        message.setSubject("Nhắc việc: " + task.title() + " sắp đến hạn");
        long minutesRemaining = Math.max(1, Duration.between(now, dueAt).toMinutes());
        message.setText("Xin chào " + task.fullName() + ",\n\n"
                + "Công việc \"" + task.title() + "\" sẽ đến hạn sau khoảng " + minutesRemaining + " phút.\n"
                + "Thời hạn: " + DATE_FORMAT.format(dueAt.atZone(zone)) + " (" + zone.getId() + ").\n\n"
                + "Mở Dayflow để xem công việc của bạn.\n\nDayflow");
        sender.send(message);
        log.info("Sent task reminder for task {} to {}", task.id(), task.email());
    }

    private ZoneId zoneFor(String timezone) {
        if (StringUtils.hasText(timezone)) {
            try {
                return ZoneId.of(timezone);
            } catch (DateTimeException ignored) {
                log.warn("Invalid timezone '{}' for task reminder; using system timezone", timezone);
            }
        }
        return ZoneId.systemDefault();
    }

    private record TaskReminder(Long id, String title, LocalDateTime dueDate, String email, String fullName, String timezone) {}
}
