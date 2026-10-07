package org.example.ingresso.ingresso.service;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
@Profile({"dev", "test"})
public class MockEmailGateway implements EmailGateway {

    private final List<PurchaseConfirmation> capturedMessages = new CopyOnWriteArrayList<>();
    private final AtomicBoolean failNext = new AtomicBoolean();

    @Override
    public void send(PurchaseConfirmation message) {
        if (failNext.compareAndSet(true, false)) {
            throw new IllegalStateException("Falha simulada no envio de e-mail");
        }
        capturedMessages.add(message);
    }

    public List<PurchaseConfirmation> getCapturedMessages() {
        return List.copyOf(capturedMessages);
    }

    public void clear() {
        capturedMessages.clear();
        failNext.set(false);
    }

    public void failNextForTest() {
        failNext.set(true);
    }
}