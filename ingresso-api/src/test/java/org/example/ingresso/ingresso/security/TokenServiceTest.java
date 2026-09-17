package org.example.ingresso.ingresso.security;

import org.example.ingresso.ingresso.model.enums.UsuarioPerfil;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class TokenServiceTest {

    @Test
    void shouldGenerateAndValidateToken() {
        TokenService tokenService = new TokenService();
        ReflectionTestUtils.setField(tokenService, "secret", "test-secret");

        UserSS user = new UserSS(1L, "admin@ingresso.com", "password", UsuarioPerfil.ADMIN);

        String token = tokenService.generateToken(user);
        String subject = tokenService.validateToken(token);

        assertNotNull(token);
        assertEquals("admin@ingresso.com", subject);
    }

    @Test
    void shouldReturnNullForInvalidToken() {
        TokenService tokenService = new TokenService();
        ReflectionTestUtils.setField(tokenService, "secret", "test-secret");

        assertNull(tokenService.validateToken("token-invalido"));
    }
}
