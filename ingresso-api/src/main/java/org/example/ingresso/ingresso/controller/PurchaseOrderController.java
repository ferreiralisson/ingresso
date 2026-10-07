package org.example.ingresso.ingresso.controller;

import org.example.ingresso.ingresso.dto.CreateOrderRequest;
import org.example.ingresso.ingresso.dto.OrderResponse;
import org.example.ingresso.ingresso.model.enums.OrderStatus;
import org.example.ingresso.ingresso.security.UserSS;
import org.example.ingresso.ingresso.service.PurchaseOrderService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/pedidos")
public class PurchaseOrderController {

    private final PurchaseOrderService orderService;

    public PurchaseOrderController(PurchaseOrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public ResponseEntity<OrderResponse> create(
        @RequestHeader("Idempotency-Key") String idempotencyKey,
        @RequestBody @Valid CreateOrderRequest request,
        @AuthenticationPrincipal UserSS buyer
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(orderService.create(request, idempotencyKey, buyer));
    }

    @PostMapping("/{orderId}/pagamento")
    public ResponseEntity<OrderResponse> startPayment(
        @PathVariable Long orderId,
        @AuthenticationPrincipal UserSS buyer
    ) {
        OrderResponse order = orderService.startPayment(orderId, buyer);
        if (order.status() == OrderStatus.PENDING_PAYMENT) {
            return ResponseEntity.accepted().body(order);
        }
        return ResponseEntity.ok(order);
    }

    @GetMapping
    public ResponseEntity<Page<OrderResponse>> listMine(
        @AuthenticationPrincipal UserSS buyer,
        @PageableDefault(size = 20, sort = "createdAt") Pageable pageable
    ) {
        return ResponseEntity.ok(orderService.listMine(buyer, pageable));
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<OrderResponse> getMine(
        @PathVariable Long orderId,
        @AuthenticationPrincipal UserSS buyer
    ) {
        return ResponseEntity.ok(orderService.getMine(orderId, buyer));
    }
}