package com.example.demo.controller;

import com.example.demo.dto.request.LoginRequest;
import com.example.demo.dto.request.RegisterRequest;
import com.example.demo.dto.request.SkinSelectionRequest;
import com.example.demo.dto.response.AuthResponse;
import com.example.demo.entity.User;
import com.example.demo.security.AppUserPrincipal;
import com.example.demo.security.TabAuthenticationStore;
import com.example.demo.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import com.example.demo.service.UserService;
import com.example.demo.entity.Skin;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository securityContextRepository;
    private final AuthService authService;
    private final UserService userService;
    private final TabAuthenticationStore tabAuthenticationStore;

    @GetMapping("/csrf")
    public CsrfResponse csrf(CsrfToken token) {
        return new CsrfResponse(token.getToken());
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        User user = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(user));
    }

    @PostMapping("/login")
    public AuthResponse login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(
                            request.email().trim().toLowerCase(), request.password()));
        } catch (DisabledException exception) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Tài khoản đã bị tạm dừng");
        } catch (AuthenticationException exception) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Email hoặc mật khẩu không đúng");
        }

        HttpSession session = httpRequest.getSession(true);
        httpRequest.changeSessionId();
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, httpRequest, httpResponse);
        AuthResponse user = toResponse((AppUserPrincipal) authentication.getPrincipal());
        return new AuthResponse(user.id(), user.email(), user.fullName(), user.role(), tabAuthenticationStore.create(authentication));
    }

    @GetMapping("/me")
    public AuthResponse me(Authentication authentication) {
        return toResponse((AppUserPrincipal) authentication.getPrincipal());
    }

    @GetMapping("/me/skin")
    public Skin mySkin(@AuthenticationPrincipal AppUserPrincipal principal) {
        return userService.getSkin(principal.id());
    }

    @PutMapping("/me/skin")
    public Skin selectSkin(
            @Valid @RequestBody SkinSelectionRequest request,
            @AuthenticationPrincipal AppUserPrincipal principal) {
        return userService.selectSkin(principal.id(), request.skinId());
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request, HttpServletResponse response) {
        String authorization = request.getHeader("Authorization");
        if (authorization != null && authorization.startsWith("Bearer ")) {
            tabAuthenticationStore.remove(authorization.substring(7));
        }
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        SecurityContextHolder.clearContext();
        return ResponseEntity.noContent().build();
    }

    private AuthResponse toResponse(User user) {
        return new AuthResponse(user.getId(), user.getEmail(), user.getFullName(), user.getRole().name());
    }

    private AuthResponse toResponse(AppUserPrincipal principal) {
        return new AuthResponse(principal.id(), principal.getUsername(), principal.fullName(), principal.role().name());
    }

    public record CsrfResponse(String token) {
    }
}
