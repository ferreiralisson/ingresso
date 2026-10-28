package org.example.ingresso.ingresso.service;

import org.example.ingresso.ingresso.service.dto.PurchaseContracts.PaymentRequest;
import org.example.ingresso.ingresso.service.dto.PurchaseContracts.PaymentResult;

public interface PaymentGateway {

    PaymentResult start(PaymentRequest request);

    String providerName();
}