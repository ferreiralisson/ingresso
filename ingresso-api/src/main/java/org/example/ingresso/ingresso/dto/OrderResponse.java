package org.example.ingresso.ingresso.dto;

import org.example.ingresso.ingresso.model.enums.OrderStatus;
import org.example.ingresso.ingresso.model.enums.PaymentOutcome;

import java.time.Instant;
import java.util.List;

public record OrderResponse(
    Long id,
    Long eventId,
    String eventTitle,
    OrderStatus status,
    long totalInCents,
    Instant createdAt,
    Instant reservationExpiresAt,
    Instant paidAt,
    List<Item> items,
    Payment payment,
    List<Ticket> tickets
) {
    public record Item(
        Long categoryId,
        String categoryCode,
        String categoryName,
        String admissionMode,
        long unitPriceInCents,
        int quantity,
        List<Seat> seats
    ) {
    }

    public record Seat(Long id, String sector, String row, String label) {
    }

    public record Payment(String attemptKey, String provider, PaymentOutcome outcome, Instant startedAt, Instant completedAt) {
    }

    public record Ticket(
        Long id,
        String categoryName,
        int unitNumber,
        Long eventSeatId,
        String sector,
        String row,
        String seatLabel,
        String qrCodeValue,
        Instant usedAt,
        Instant refundedAt,
        Instant issuedAt,
        String eventTitle,
        Instant eventStartsAt,
        String venueName,
        String streetAddress,
        String city,
        String stateCode
    ) {
    }
}