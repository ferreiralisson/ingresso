package org.example.ingresso.ingresso.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTCreationException;
import com.auth0.jwt.exceptions.JWTVerificationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Service
public class TokenService {

    private static final String ISSUER = "ingresso-api";

    @Value("${token.secret}")
    private String secret;

    public String generateToken(UserSS userSS) {
        try {
            Algorithm algorithm = Algorithm.HMAC512(secret);
            return JWT.create()
                .withIssuer(ISSUER)
                .withSubject(userSS.getUsername())
                .withExpiresAt(genExpirationDate())
                .sign(algorithm);
        } catch (JWTCreationException exception) {
            throw new IllegalStateException("Erro ao gerar token", exception);
        }
    }

    public String validateToken(String token) {
        try {
            Algorithm algorithm = Algorithm.HMAC512(secret);
            return JWT.require(algorithm)
                .withIssuer(ISSUER)
                .build()
                .verify(token)
                .getSubject();
        } catch (JWTVerificationException exception) {
            return null;
        }
    }

    private Instant genExpirationDate() {
        return LocalDateTime.now().plusHours(2).toInstant(ZoneOffset.of("-03:00"));
    }
}
