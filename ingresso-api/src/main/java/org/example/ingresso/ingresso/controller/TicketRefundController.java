package org.example.ingresso.ingresso.controller;

import org.example.ingresso.ingresso.dto.TicketRefundRequest;
import org.example.ingresso.ingresso.dto.TicketRefundResponse;
import org.example.ingresso.ingresso.security.UserSS;
import org.example.ingresso.ingresso.service.TicketRefundService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/pedidos")
public class TicketRefundController {

    private final TicketRefundService ticketRefundService;

    public TicketRefundController(TicketRefundService ticketRefundService) {
        this.ticketRefundService = ticketRefundService;
    }

    @PostMapping("/{orderId}/tickets/{ticketId}/reembolsos")
    public ResponseEntity<TicketRefundResponse> requestRefund(
        @PathVariable Long orderId,
        @PathVariable Long ticketId,
        @RequestBody(required = false) @Valid TicketRefundRequest request,
        @AuthenticationPrincipal UserSS buyer
    ) {
        return ResponseEntity.status(201).body(ticketRefundService.requestBuyerRefund(
            orderId, ticketId, request == null ? null : request.reason(), buyer
        ));
    }
}