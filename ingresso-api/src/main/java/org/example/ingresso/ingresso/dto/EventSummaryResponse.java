package org.example.ingresso.ingresso.dto;

import org.example.ingresso.ingresso.model.enums.EventStatus;

import java.time.OffsetDateTime;

public record EventSummaryResponse(
    Long id,
    String title,
    String imageUrl,
    OffsetDateTime startsAt,
    String city,
    String stateCode,
    long lowestPriceInCents,
    EventStatus status
) {
}