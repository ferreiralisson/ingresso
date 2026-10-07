package org.example.ingresso.ingresso.dto;

import java.util.List;

public record AdminFinancialReport(
    long paidOrderCount,
    long paidTicketCount,
    long refundedTicketCount,
    long grossSalesInCents,
    long simulatedRefundsInCents,
    long remainingGrossInCents,
    List<EventFinancialReport> events
) {
}