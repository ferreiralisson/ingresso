package org.example.ingresso.ingresso.dto;

import java.time.Instant;

public record EventStaffGrantResponse(Long id, Long userId, String email, Instant acceptedAt) {
}