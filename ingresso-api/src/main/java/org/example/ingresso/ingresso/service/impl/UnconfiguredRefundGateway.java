package org.example.ingresso.ingresso.service.impl;

import org.example.ingresso.ingresso.service.RefundGateway;
import org.example.ingresso.ingresso.service.dto.RefundContracts.RefundRequest;
import org.example.ingresso.ingresso.service.dto.RefundContracts.RefundResult;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("!dev & !test")
public class UnconfiguredRefundGateway implements RefundGateway {

    @Override
    public RefundResult refund(RefundRequest request) {
        throw new IllegalStateException("Nenhum provedor de reembolso esta configurado");
    }
}