package org.example.ingresso.ingresso.service;

public interface EmailGateway {
    void send(PurchaseConfirmation message);
}