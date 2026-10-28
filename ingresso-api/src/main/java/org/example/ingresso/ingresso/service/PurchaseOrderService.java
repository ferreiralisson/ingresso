package org.example.ingresso.ingresso.service;

import org.example.ingresso.ingresso.service.dto.PageQuery;
import org.example.ingresso.ingresso.service.dto.PageResult;
import org.example.ingresso.ingresso.service.dto.PurchaseContracts.CreateOrderCommand;
import org.example.ingresso.ingresso.service.dto.PurchaseContracts.OrderView;
import org.example.ingresso.ingresso.service.dto.PurchaseContracts.PaymentResult;

public interface PurchaseOrderService {

    OrderView create(CreateOrderCommand request, String idempotencyKey, Long buyerId);

    OrderView startPayment(Long orderId, Long buyerId);

    PageResult<OrderView> listMine(Long buyerId, PageQuery query);

    OrderView getMine(Long orderId, Long buyerId);

    void cancelPendingForEvent(Long eventId);

    OrderView processPaymentResult(Long orderId, String attemptKey, PaymentResult outcome);

    void expireScheduledOrders();
}