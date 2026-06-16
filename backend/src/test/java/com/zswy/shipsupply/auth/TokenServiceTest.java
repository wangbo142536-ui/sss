package com.zswy.shipsupply.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

class TokenServiceTest {

    @Test
    void resolvesIssuedTokenAfterServiceRestart() {
        AuthRepository authRepository = org.mockito.Mockito.mock(AuthRepository.class);
        Map<String, Long> persistedTokens = new HashMap<>();
        doAnswer(invocation -> {
            persistedTokens.put(invocation.getArgument(0), invocation.getArgument(1));
            return null;
        }).when(authRepository).saveToken(anyString(), anyLong(), any(LocalDateTime.class));
        when(authRepository.findValidTokenUserId(anyString())).thenAnswer(invocation ->
            Optional.ofNullable(persistedTokens.get(invocation.getArgument(0)))
        );

        TokenService beforeRestart = new TokenService(authRepository);
        String token = beforeRestart.issue(20L);
        TokenService afterRestart = new TokenService(authRepository);

        assertThat(afterRestart.requireUserId("Bearer " + token)).isEqualTo(20L);
    }

    @Test
    void rejectsUnknownToken() {
        AuthRepository authRepository = org.mockito.Mockito.mock(AuthRepository.class);
        when(authRepository.findValidTokenUserId("missing-token")).thenReturn(Optional.empty());

        TokenService tokenService = new TokenService(authRepository);

        assertThatThrownBy(() -> tokenService.requireUserId("Bearer missing-token"))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("401 UNAUTHORIZED")
            .hasMessageContaining("Invalid token");
    }
}
