package com.example.demo.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class ActiveAccountInterceptor implements HandlerInterceptor {
    private final JdbcTemplate jdbc;

    public ActiveAccountInterceptor(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AppUserPrincipal principal)) {
            return true;
        }
        List<Boolean> status = jdbc.queryForList("SELECT is_active = 1 FROM users WHERE id = ?", Boolean.class, principal.id());
        if (!status.isEmpty() && Boolean.TRUE.equals(status.getFirst())) return true;

        SecurityContextHolder.clearContext();
        HttpSession session = request.getSession(false);
        if (session != null) session.invalidate();
        response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Tài khoản đã bị tạm dừng");
        return false;
    }
}
