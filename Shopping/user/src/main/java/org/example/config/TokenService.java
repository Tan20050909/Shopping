package org.example.config;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class TokenService {
    private final Map<String, Long> tokens = new ConcurrentHashMap<>();

    public String issue(long userId) {
        String token = "demo-" + userId + "-" + UUID.randomUUID();
        tokens.put(token, userId);
        return token;
    }

    public Long resolve(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return null;
        }
        String token = rawToken.startsWith("Bearer ") ? rawToken.substring(7) : rawToken;
        return tokens.get(token);
    }
}
