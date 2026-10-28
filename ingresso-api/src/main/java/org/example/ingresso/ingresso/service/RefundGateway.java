package org.example.ingresso.ingresso.service;

import org.example.ingresso.ingresso.service.dto.RefundContracts.RefundRequest;
import org.example.ingresso.ingresso.service.dto.RefundContracts.RefundResult;

public interface RefundGateway {
    RefundResult refund(RefundRequest request);
}