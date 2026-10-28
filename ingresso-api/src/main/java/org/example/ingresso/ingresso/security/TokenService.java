package org.example.ingresso.ingresso.security;

public interface TokenService {

    String generateToken(String username);

    String validateToken(String token);
}
