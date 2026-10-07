package org.example.ingresso.ingresso.dto;

import java.time.Instant;

public record ProducerInvitationResponse(
    Long id,
    String email,
    String token,
    Instant expiresAt
) {
}