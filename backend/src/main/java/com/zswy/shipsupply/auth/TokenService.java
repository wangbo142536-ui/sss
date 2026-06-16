package com.zswy.shipsupply.auth;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class TokenService {

    private static final int TOKEN_TTL_DAYS = 7;

    private final SecureRandom secureRandom = new SecureRandom();
    private final Map<String, Long> userIdsByToken = new ConcurrentHashMap<>();
    private final AuthRepository authRepository;

    public TokenService(AuthRepository authRepository) {
        this.authRepository = authRepository;
    }

    public String issue(Long userId) {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        userIdsByToken.put(token, userId);
        authRepository.saveToken(token, userId, LocalDateTime.now().plusDays(TOKEN_TTL_DAYS));
        return token;
    }

    public Long requireUserId(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            throw invalidToken();
        }
        String token = authorizationHeader.substring("Bearer ".length()).trim();
        if (token.isBlank()) {
            throw invalidToken();
        }
        Long userId = userIdsByToken.get(token);
        if (userId != null) {
            return userId;
        }
        Long persistedUserId = authRepository.findValidTokenUserId(token)
            .orElseThrow(TokenService::invalidToken);
        userIdsByToken.put(token, persistedUserId);
        return persistedUserId;
    }

    private static ResponseStatusException invalidToken() {
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid token");
    }
}
