package org.example.ingresso.ingresso.dto;

import org.example.ingresso.ingresso.model.enums.TicketRefundSource;
import org.example.ingresso.ingresso.model.enums.TicketRefundStatus;

import java.time.Instant;

public record TicketRefundResponse(
    Long id,
    Long ticketId,
    Long orderId,
    Long eventId,
    TicketRefundSource source,
    TicketRefundStatus status,
    long amountInCents,
    String reason,
    Instant createdAt
) {
}