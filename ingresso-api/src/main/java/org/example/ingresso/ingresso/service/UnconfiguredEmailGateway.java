package org.example.ingresso.ingresso.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("!dev & !test")
public class UnconfiguredEmailGateway implements EmailGateway {

    private static final Logger logger = LoggerFactory.getLogger(UnconfiguredEmailGateway.class);

    @Override
    public void send(PurchaseConfirmation message) {
        logger.warn("No email provider configured; confirmation for order {} was not sent", message.orderId());
    }
}