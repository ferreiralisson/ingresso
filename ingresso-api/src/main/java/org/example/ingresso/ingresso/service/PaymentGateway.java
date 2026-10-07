package org.example.ingresso.ingresso.service;

import org.example.ingresso.ingresso.model.PurchaseOrder;
import org.example.ingresso.ingresso.model.enums.PaymentOutcome;

public interface PaymentGateway {

    PaymentOutcome start(PurchaseOrder order);

    String providerName();
}