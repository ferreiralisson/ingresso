package org.example.ingresso.ingresso.service;

import org.example.ingresso.ingresso.service.dto.RefundContracts.TicketRefundView;

public interface TicketRefundService {

    TicketRefundView requestBuyerRefund(Long orderId, Long ticketId, String reason, Long buyerId);

    void refundUnusedTicketsForEvent(Long eventId, Long actorId);
}