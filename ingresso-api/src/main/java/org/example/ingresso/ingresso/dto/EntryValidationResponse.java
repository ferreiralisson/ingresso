package org.example.ingresso.ingresso.dto;

import org.example.ingresso.ingresso.model.enums.EntryCheckInOutcome;
import org.example.ingresso.ingresso.model.enums.EntryCheckInSource;

import java.time.Instant;

public record EntryValidationResponse(
    EntryCheckInOutcome outcome,
    String message,
    Long ticketId,
    Long checkInId,
    Instant processedAt,
    Instant previousUseAt,
    String previousOperator,
    EntryCheckInSource source,
    Instant deviceScannedAt,
    Instant synchronizedAt,
    String operatorEmail,
    Instant correctedAt,
    String correctionReason,
    String correctedBy
) {
}