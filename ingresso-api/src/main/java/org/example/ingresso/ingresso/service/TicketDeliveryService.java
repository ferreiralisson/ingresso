package org.example.ingresso.ingresso.service;

import org.example.ingresso.ingresso.dto.OrderResponse;
import org.example.ingresso.ingresso.model.IssuedTicket;
import org.example.ingresso.ingresso.model.OrderItem;
import org.example.ingresso.ingresso.model.OrderSeatSnapshot;
import org.example.ingresso.ingresso.model.PurchaseOrder;
import org.example.ingresso.ingresso.model.enums.OrderStatus;
import org.example.ingresso.ingresso.repository.IssuedTicketRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class TicketDeliveryService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final IssuedTicketRepository ticketRepository;
    private final ApplicationEventPublisher eventPublisher;

    public TicketDeliveryService(IssuedTicketRepository ticketRepository, ApplicationEventPublisher eventPublisher) {
        this.ticketRepository = ticketRepository;
        this.eventPublisher = eventPublisher;
    }

    public void issueForPaidOrder(PurchaseOrder order) {
        if (order.getStatus() != OrderStatus.PAID) {
            return;
        }
        if (ticketRepository.countByOrder_Id(order.getId()) != 0 || !order.getTickets().isEmpty()) {
            return;
        }
        if (order.getItems().isEmpty()) {
            throw new IllegalStateException("Pedido pago nao possui itens para emitir ingressos");
        }

        Set<Long> seatIds = new HashSet<>();
        for (OrderItem item : order.getItems()) {
            if (item.getQuantity() <= 0 || item.getQuantity() > 10) {
                throw new IllegalStateException("Quantidade inconsistente no item do pedido");
            }
            if (item.getAdmissionModeSnapshot().equals("ASSIGNED_SEAT")) {
                if (item.getSeats().size() != item.getQuantity()) {
                    throw new IllegalStateException("Snapshot de assentos inconsistente no pedido pago");
                }
                for (OrderSeatSnapshot seat : item.getSeats()) {
                    if (!seatIds.add(seat.getEventSeatId())) {
                        throw new IllegalStateException("Assento duplicado no pedido pago");
                    }
                }
            } else if (item.getAdmissionModeSnapshot().equals("GENERAL_ADMISSION")) {
                if (!item.getSeats().isEmpty()) {
                    throw new IllegalStateException("Categoria geral contem snapshot de assentos");
                }
            } else {
                throw new IllegalStateException("Modalidade de ingresso desconhecida");
            }
        }

        Instant issuedAt = Instant.now();
        List<IssuedTicket> issuedTickets = new ArrayList<>();
        for (OrderItem item : order.getItems()) {
            for (int unit = 0; unit < item.getQuantity(); unit++) {
                OrderSeatSnapshot seat = item.getSeats().isEmpty() ? null : item.getSeats().get(unit);
                String qrToken = newQrToken();
                issuedTickets.add(new IssuedTicket(order, item, unit + 1, seat, qrToken, hash(qrToken), issuedAt));
            }
        }
        issuedTickets.forEach(order::addTicket);
        ticketRepository.saveAll(issuedTickets);

        var event = order.getEvent();
        eventPublisher.publishEvent(new PurchaseConfirmation(
            order.getId(),
            order.getBuyer().getEmail(),
            order.getBuyer().getNome(),
            event.getTitle(),
            event.getStartsAt(),
            event.getVenueName(),
            event.getStreetAddress(),
            event.getCity(),
            event.getStateCode(),
            issuedTickets.stream().map(ticket -> new PurchaseConfirmation.Ticket(
                ticket.getOrderItem().getCategoryNameSnapshot(),
                ticket.getUnitNumber(),
                ticket.getSector(),
                ticket.getRow(),
                ticket.getSeatLabel(),
                ticket.getQrToken()
            )).toList()
        ));
    }

    public List<OrderResponse.Ticket> toResponse(PurchaseOrder order) {
        if (order.getStatus() != OrderStatus.PAID) {
            return List.of();
        }
        var event = order.getEvent();
        return order.getTickets().stream().map(ticket -> new OrderResponse.Ticket(
            ticket.getId(),
            ticket.getOrderItem().getCategoryNameSnapshot(),
            ticket.getUnitNumber(),
            ticket.getEventSeatId(),
            ticket.getSector(),
            ticket.getRow(),
            ticket.getSeatLabel(),
            ticket.isRefunded() ? null : ticket.getQrToken(),
            ticket.getUsedAt(),
            ticket.getRefundedAt(),
            ticket.getIssuedAt(),
            event.getTitle(),
            event.getStartsAt(),
            event.getVenueName(),
            event.getStreetAddress(),
            event.getCity(),
            event.getStateCode()
        )).toList();
    }

    private String newQrToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hash(String value) {
        try {
            return java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256")
                .digest(value.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        } catch (java.security.NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 indisponivel", exception);
        }
    }
}