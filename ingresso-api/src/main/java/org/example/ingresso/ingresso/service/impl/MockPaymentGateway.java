package org.example.ingresso.ingresso.service.impl;

import org.example.ingresso.ingresso.service.PaymentGateway;
import org.example.ingresso.ingresso.model.enums.PaymentOutcome;
import org.example.ingresso.ingresso.service.dto.PurchaseContracts.PaymentRequest;
import org.example.ingresso.ingresso.service.dto.PurchaseContracts.PaymentResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicReference;

@Component
@Profile({"dev", "test"})
public class MockPaymentGateway implements PaymentGateway {

    private final PaymentOutcome configuredOutcome;
    private final AtomicReference<PaymentOutcome> nextOutcome = new AtomicReference<>();

    public MockPaymentGateway(@Value("${ingresso.payment.mock.default-result:APPROVED}") PaymentOutcome configuredOutcome) {
        this.configuredOutcome = configuredOutcome;
    }

    @Override
    public PaymentResult start(PaymentRequest request) {
        PaymentOutcome testOutcome = nextOutcome.getAndSet(null);
        return PaymentResult.valueOf((testOutcome == null ? configuredOutcome : testOutcome).name());
    }

    @Override
    public String providerName() {
        return "mock";
    }

    public void setNextOutcomeForTest(PaymentOutcome outcome) {
        nextOutcome.set(outcome);
    }
}