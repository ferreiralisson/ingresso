package org.example.ingresso.ingresso.security;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class TokenServiceTest {

    @Test
    void shouldGenerateAndValidateToken() {
        TokenServiceImpl tokenService = new TokenServiceImpl();
        ReflectionTestUtils.setField(tokenService, "secret", "test-secret");

        String token = tokenService.generateToken("admin@ingresso.com");
        String subject = tokenService.validateToken(token);

        assertNotNull(token);
        assertEquals("admin@ingresso.com", subject);
    }

    @Test
    void shouldReturnNullForInvalidToken() {
        TokenServiceImpl tokenService = new TokenServiceImpl();
        ReflectionTestUtils.setField(tokenService, "secret", "test-secret");

        assertNull(tokenService.validateToken("token-invalido"));
    }

    @Test
    void shouldRejectJwtSecretShorterThan512Bits() {
        TokenServiceImpl tokenService = new TokenServiceImpl();
        ReflectionTestUtils.setField(tokenService, "secret", "short-secret");

        assertThrows(IllegalStateException.class, tokenService::validateSecret);
    }
}
