package org.example.ingresso.ingresso.controller;

import org.example.ingresso.ingresso.dto.CreateOrderRequest;
import org.example.ingresso.ingresso.dto.OrderResponse;
import org.example.ingresso.ingresso.model.enums.OrderStatus;
import org.example.ingresso.ingresso.model.enums.PaymentOutcome;
import org.example.ingresso.ingresso.service.dto.PurchaseContracts.CreateOrderCommand;
import org.example.ingresso.ingresso.service.dto.PurchaseContracts.ItemCommand;
import org.example.ingresso.ingresso.service.dto.PurchaseContracts.OrderView;
import org.example.ingresso.ingresso.service.dto.TicketDeliveryContracts.TicketView;

import java.util.List;

final class PurchaseHttpMapper {

    private PurchaseHttpMapper() {
    }

    static CreateOrderCommand toCommand(CreateOrderRequest request) {
        return new CreateOrderCommand(
            request.eventId(),
            request.items().stream()
                .map(item -> new ItemCommand(item.categoryId(), item.quantity(), item.seatIds()))
                .toList()
        );
    }

    static OrderResponse toResponse(OrderView order) {
        List<OrderResponse.Item> items = order.items().stream()
            .map(item -> new OrderResponse.Item(
                item.categoryId(), item.categoryCode(), item.categoryName(), item.admissionMode(),
                item.unitPriceInCents(), item.quantity(), item.seats().stream()
                    .map(seat -> new OrderResponse.Seat(seat.id(), seat.sector(), seat.row(), seat.label()))
                    .toList()
            ))
            .toList();
        List<OrderResponse.Ticket> tickets = order.tickets().stream()
            .map(PurchaseHttpMapper::toTicketResponse)
            .toList();
        OrderResponse.Payment payment = order.payment() == null ? null : new OrderResponse.Payment(
            order.payment().attemptKey(), order.payment().provider(), PaymentOutcome.valueOf(order.payment().outcome()),
            order.payment().startedAt(), order.payment().completedAt()
        );
        return new OrderResponse(
            order.id(), order.eventId(), order.eventTitle(), OrderStatus.valueOf(order.status()), order.totalInCents(),
            order.createdAt(), order.reservationExpiresAt(), order.paidAt(), items, payment, tickets
        );
    }

    private static OrderResponse.Ticket toTicketResponse(TicketView ticket) {
        return new OrderResponse.Ticket(
            ticket.id(), ticket.categoryName(), ticket.unitNumber(), ticket.eventSeatId(), ticket.sector(), ticket.row(),
            ticket.seatLabel(), ticket.qrCodeValue(), ticket.usedAt(), ticket.refundedAt(), ticket.issuedAt(),
            ticket.eventTitle(), ticket.eventStartsAt(), ticket.venueName(), ticket.streetAddress(), ticket.city(),
            ticket.stateCode()
        );
    }
}
