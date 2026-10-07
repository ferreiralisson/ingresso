package org.example.ingresso.ingresso.service;

import org.example.ingresso.ingresso.model.enums.TicketRefundSource;
import org.example.ingresso.ingresso.model.enums.TicketRefundStatus;

public interface RefundGateway {
    TicketRefundStatus refund(Long ticketId, long amountInCents, TicketRefundSource source);
}