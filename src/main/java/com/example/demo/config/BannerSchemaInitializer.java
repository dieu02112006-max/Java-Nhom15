package com.example.demo.config;

import java.util.HashSet;
import java.util.Set;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** Tạo bảng banners (ảnh banner cho trang giới thiệu) và nạp 3 banner mẫu nếu chưa có ảnh nào. */
@Component
public class BannerSchemaInitializer implements ApplicationRunner {

    private final JdbcTemplate jdbc;

    public BannerSchemaInitializer(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void run(ApplicationArguments args) {
        jdbc.execute("""
                CREATE TABLE IF NOT EXISTS banners (
                    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
                    title VARCHAR(200) NOT NULL,
                    image_url VARCHAR(500) DEFAULT NULL,
                    link_url VARCHAR(300) DEFAULT NULL,
                    sort_order INT NOT NULL DEFAULT 0,
                    is_active TINYINT(1) NOT NULL DEFAULT 1,
                    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
                ) CHARACTER SET utf8mb4
                """);
        // Bảng từ phiên bản cũ (banner dạng chữ) chưa có cột ảnh -> bổ sung.
        Set<String> columns = new HashSet<>(jdbc.queryForList("""
                SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS
                WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'banners'
                """, String.class));
        if (!columns.contains("image_url")) {
            jdbc.execute("ALTER TABLE banners ADD COLUMN image_url VARCHAR(500) DEFAULT NULL AFTER title");
        }
        if (!columns.contains("link_url")) {
            jdbc.execute("ALTER TABLE banners ADD COLUMN link_url VARCHAR(300) DEFAULT NULL AFTER image_url");
        }
        Integer withImage = jdbc.queryForObject("SELECT COUNT(*) FROM banners WHERE image_url IS NOT NULL", Integer.class);
        if (withImage == null || withImage == 0) {
            jdbc.update("DELETE FROM banners");
            String sql = "INSERT INTO banners (title, image_url, link_url, sort_order, is_active) VALUES (?, ?, NULL, ?, 1)";
            jdbc.update(sql, "Sắp xếp ngày. Sống chủ động.", "/home/banners/banner-1.png", 1);
            jdbc.update(sql, "Hiểu rõ tiền. Chi tiêu thông minh.", "/home/banners/banner-2.png", 2);
            jdbc.update(sql, "Việc nhỏ mỗi ngày. Kết quả lớn.", "/home/banners/banner-3.png", 3);
        }
    }
}
