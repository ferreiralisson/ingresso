package org.example.ingresso.ingresso.service;

import java.time.Instant;
import java.util.List;

public record PurchaseConfirmation(
    Long orderId,
    String recipient,
    String buyerName,
    String eventTitle,
    Instant eventStartsAt,
    String venueName,
    String streetAddress,
    String city,
    String stateCode,
    List<Ticket> tickets
) {
    public record Ticket(
        String categoryName,
        int unitNumber,
        String sector,
        String row,
        String seatLabel,
        String qrCodeValue
    ) {
    }
}