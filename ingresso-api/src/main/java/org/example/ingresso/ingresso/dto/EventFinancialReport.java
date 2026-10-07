package org.example.ingresso.ingresso.dto;

public record EventFinancialReport(
    Long eventId,
    String eventTitle,
    long paidOrderCount,
    long paidTicketCount,
    long refundedTicketCount,
    long grossSalesInCents,
    long simulatedRefundsInCents,
    long remainingGrossInCents
) {
}