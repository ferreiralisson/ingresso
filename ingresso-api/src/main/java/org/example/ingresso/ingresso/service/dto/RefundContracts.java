package org.example.ingresso.ingresso.service.dto;

import java.time.Instant;

public final class RefundContracts {

    private RefundContracts() {
    }

    public record RefundRequest(Long ticketId, long amountInCents, String source) {
    }

    public record RefundResult(String status) {
    }

    public record TicketRefundView(
        Long id,
        Long ticketId,
        Long orderId,
        Long eventId,
        String source,
        String status,
        long amountInCents,
        String reason,
        Instant createdAt
    ) {
    }
}
