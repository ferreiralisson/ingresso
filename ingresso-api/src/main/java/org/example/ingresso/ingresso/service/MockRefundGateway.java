package org.example.ingresso.ingresso.service;

import org.example.ingresso.ingresso.model.enums.TicketRefundSource;
import org.example.ingresso.ingresso.model.enums.TicketRefundStatus;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile({"dev", "test"})
public class MockRefundGateway implements RefundGateway {

    @Override
    public TicketRefundStatus refund(Long ticketId, long amountInCents, TicketRefundSource source) {
        return TicketRefundStatus.SIMULATED;
    }
}