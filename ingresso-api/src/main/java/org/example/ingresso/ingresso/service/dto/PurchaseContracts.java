package org.example.ingresso.ingresso.service.dto;

import java.time.Instant;
import java.util.List;

public final class PurchaseContracts {

    private PurchaseContracts() {
    }

    public record CreateOrderCommand(Long eventId, List<ItemCommand> items) {
        public CreateOrderCommand {
            items = List.copyOf(items);
        }
    }

    public record ItemCommand(Long categoryId, int quantity, List<Long> seatIds) {
        public ItemCommand {
            seatIds = seatIds == null ? List.of() : List.copyOf(seatIds);
        }
    }

    public record OrderView(
        Long id,
        Long eventId,
        String eventTitle,
        String status,
        long totalInCents,
        Instant createdAt,
        Instant reservationExpiresAt,
        Instant paidAt,
        List<ItemView> items,
        PaymentView payment,
        List<TicketDeliveryContracts.TicketView> tickets
    ) {
        public OrderView {
            items = List.copyOf(items);
            tickets = List.copyOf(tickets);
        }
    }

    public record ItemView(
        Long categoryId,
        String categoryCode,
        String categoryName,
        String admissionMode,
        long unitPriceInCents,
        int quantity,
        List<SeatView> seats
    ) {
        public ItemView {
            seats = List.copyOf(seats);
        }
    }

    public record SeatView(Long id, String sector, String row, String label) {
    }

    public record PaymentView(
        String attemptKey,
        String provider,
        String outcome,
        Instant startedAt,
        Instant completedAt
    ) {
    }

    public record PaymentRequest(Long orderId, long totalInCents) {
    }

    public enum PaymentResult {
        APPROVED,
        DECLINED,
        PENDING
    }
}