package org.example.ingresso.ingresso.service;

import org.example.ingresso.ingresso.model.PurchaseOrder;
import org.example.ingresso.ingresso.model.enums.PaymentOutcome;
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
    public PaymentOutcome start(PurchaseOrder order) {
        PaymentOutcome testOutcome = nextOutcome.getAndSet(null);
        return testOutcome == null ? configuredOutcome : testOutcome;
    }

    @Override
    public String providerName() {
        return "mock";
    }

    public void setNextOutcomeForTest(PaymentOutcome outcome) {
        nextOutcome.set(outcome);
    }
}