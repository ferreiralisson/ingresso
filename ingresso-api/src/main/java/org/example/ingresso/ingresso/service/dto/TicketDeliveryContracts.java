package org.example.ingresso.ingresso.service.dto;

import java.time.Instant;

public final class TicketDeliveryContracts {

    private TicketDeliveryContracts() {
    }

    public record TicketView(
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