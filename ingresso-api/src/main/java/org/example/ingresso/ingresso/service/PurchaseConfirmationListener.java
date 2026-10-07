package org.example.ingresso.ingresso.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class PurchaseConfirmationListener {

    private static final Logger logger = LoggerFactory.getLogger(PurchaseConfirmationListener.class);

    private final EmailGateway emailGateway;

    public PurchaseConfirmationListener(EmailGateway emailGateway) {
        this.emailGateway = emailGateway;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void deliver(PurchaseConfirmation message) {
        try {
            emailGateway.send(message);
        } catch (RuntimeException exception) {
            logger.error("Could not send purchase confirmation for order {}", message.orderId(), exception);
        }
    }
}