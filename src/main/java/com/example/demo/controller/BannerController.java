package com.example.demo.controller;

import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** API công khai: banner đang bật cho trang giới thiệu (không cần đăng nhập). */
@RestController
@RequestMapping("/api/public/banners")
public class BannerController {

    private final JdbcTemplate jdbc;

    public BannerController(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @GetMapping
    public List<Map<String, Object>> active() {
        return jdbc.queryForList("""
                SELECT id, title, image_url AS imageUrl, link_url AS linkUrl
                FROM banners
                WHERE is_active = 1 AND image_url IS NOT NULL
                ORDER BY sort_order, id
                """);
    }
}
