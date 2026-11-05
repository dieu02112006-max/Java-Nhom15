package com.example.demo.security;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

/** Keeps independent bearer sessions for browser tabs. */
@Component
public class TabAuthenticationStore {
    private final ConcurrentHashMap<String, Authentication> sessions = new ConcurrentHashMap<>();

    public String create(Authentication authentication) {
        String token = UUID.randomUUID().toString() + UUID.randomUUID();
        sessions.put(token, authentication);
        return token;
    }

    public Authentication find(String token) { return sessions.get(token); }
    public void remove(String token) { sessions.remove(token); }
}
