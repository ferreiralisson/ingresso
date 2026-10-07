package org.example.ingresso.ingresso.dto;

import java.time.Instant;

public record EventStaffInviteResponse(Long id, Long eventId, String email, String token, Instant expiresAt) {
}