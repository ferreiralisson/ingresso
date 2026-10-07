package org.example.ingresso.ingresso.dto;

import java.time.Instant;
import java.util.List;

public record OfflineManifestResponse(
    String id,
    Long eventId,
    String operatorEmail,
    String eventTitle,
    Instant eventStartsAt,
    Instant createdAt,
    Instant expiresAt,
    List<Ticket> tickets
) {
    public record Ticket(
        Long id,
        String qrTokenHash,
        boolean used,
        String categoryName,
        int unitNumber,
        String sector,
        String row,
        String seatLabel
    ) {
    }
}