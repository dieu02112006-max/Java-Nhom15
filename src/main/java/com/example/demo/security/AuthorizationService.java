package com.example.demo.security;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component("authz")
public class AuthorizationService {
    public boolean isAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
    }

    public boolean canAccessUser(Long userId) {
        if (userId == null) return false;
        if (isAdmin()) return true;
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AppUserPrincipal principal)) {
            return false;
        }
        return principal.id().equals(userId);
    }

    public void requireUserAccess(Long userId) {
        if (!canAccessUser(userId)) {
            throw new AccessDeniedException("Bạn không có quyền truy cập dữ liệu này");
        }
    }

    public boolean canAccessOwnedOrSystem(Long userId) {
        return userId == null ? isAdmin() : canAccessUser(userId);
    }

    public void requireOwnedOrAdmin(Long userId) {
        if (!canAccessOwnedOrSystem(userId)) {
            throw new AccessDeniedException("Bạn không có quyền truy cập dữ liệu này");
        }
    }

    public Long currentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof AppUserPrincipal principal) {
            return principal.id();
        }
        throw new AccessDeniedException("Yêu cầu đăng nhập");
    }
}
