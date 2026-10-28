package org.example.ingresso.ingresso.service.impl;

import org.example.ingresso.ingresso.service.TicketRefundService;
import org.example.ingresso.ingresso.service.RefundGateway;
import org.example.ingresso.ingresso.service.dto.RefundContracts.TicketRefundView;
import org.example.ingresso.ingresso.service.dto.RefundContracts.RefundRequest;
import org.example.ingresso.ingresso.service.exception.ApplicationServiceException;
import org.example.ingresso.ingresso.model.IssuedTicket;
import org.example.ingresso.ingresso.model.TicketRefund;
import org.example.ingresso.ingresso.model.Usuario;
import org.example.ingresso.ingresso.model.enums.OrderStatus;
import org.example.ingresso.ingresso.model.enums.TicketRefundSource;
import org.example.ingresso.ingresso.repository.IssuedTicketRepository;
import org.example.ingresso.ingresso.repository.EventRepository;
import org.example.ingresso.ingresso.repository.TicketRefundRepository;
import org.example.ingresso.ingresso.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class TicketRefundServiceImpl implements TicketRefundService {

    private final IssuedTicketRepository ticketRepository;
    private final TicketRefundRepository refundRepository;
    private final UsuarioRepository usuarioRepository;
    private final RefundGateway refundGateway;
    private final EventRepository eventRepository;

    public TicketRefundServiceImpl(
        IssuedTicketRepository ticketRepository,
        TicketRefundRepository refundRepository,
        UsuarioRepository usuarioRepository,
        RefundGateway refundGateway,
        EventRepository eventRepository
    ) {
        this.ticketRepository = ticketRepository;
        this.refundRepository = refundRepository;
        this.usuarioRepository = usuarioRepository;
        this.refundGateway = refundGateway;
        this.eventRepository = eventRepository;
    }

    @Transactional
    public TicketRefundView requestBuyerRefund(Long orderId, Long ticketId, String reason, Long buyerId) {
        IssuedTicket ticket = ticketRepository.findWithLockById(ticketId)
            .orElseThrow(() -> new IllegalArgumentException("Ingresso nao encontrado"));
        if (!ticket.getOrder().getId().equals(orderId)
            || !ticket.getOrder().getBuyer().getId().equals(buyerId)) {
            throw new ApplicationServiceException(
                ApplicationServiceException.Code.FORBIDDEN, "Ingresso nao pertence ao comprador autenticado"
            );
        }
        var existing = refundRepository.findByTicket_Id(ticketId);
        if (existing.isPresent()) {
            return response(existing.get());
        }

        Instant now = Instant.now();
        if (ticket.getOrder().getStatus() != OrderStatus.PAID) {
            throw new IllegalArgumentException("Somente ingressos de pedidos pagos podem ser reembolsados");
        }
        if (ticket.isUsed() || ticket.isRefunded()) {
            throw new IllegalArgumentException("Ingresso usado ou ja reembolsado nao pode ser reembolsado");
        }
        if (!ticket.getOrder().getEvent().getStartsAt().isAfter(now)) {
            throw new IllegalArgumentException("Solicitacao de reembolso encerrada no inicio do evento");
        }

        Usuario buyer = usuarioRepository.findWithPerfisById(buyerId)
            .orElseThrow(() -> new ApplicationServiceException(
                ApplicationServiceException.Code.UNAUTHORIZED, "Comprador autenticado nao encontrado"
            ));
        return response(createRefund(ticket, buyer, TicketRefundSource.BUYER_REQUEST,
            normalizeReason(reason), now));
    }

    @Transactional
    public void refundUnusedTicketsForEvent(Long eventId, Long actorId) {
        eventRepository.findById(eventId)
            .orElseThrow(() -> new IllegalArgumentException("Evento nao encontrado"));
        Usuario actor = usuarioRepository.findWithPerfisById(actorId)
            .orElseThrow(() -> new IllegalArgumentException("Usuario autenticado nao encontrado"));
        List<IssuedTicket> tickets = ticketRepository.findByOrder_Event_IdOrderByIdAsc(eventId);
        for (IssuedTicket candidate : tickets) {
            IssuedTicket ticket = ticketRepository.findWithLockById(candidate.getId()).orElse(null);
            if (ticket == null || ticket.getOrder().getStatus() != OrderStatus.PAID
                || ticket.isUsed() || ticket.isRefunded()
                || refundRepository.findByTicket_Id(ticket.getId()).isPresent()) {
                continue;
            }
            createRefund(ticket, actor, TicketRefundSource.EVENT_CANCELLATION, "Evento cancelado", Instant.now());
        }
    }

    private TicketRefund createRefund(
        IssuedTicket ticket,
        Usuario actor,
        TicketRefundSource source,
        String reason,
        Instant now
    ) {
        ticket.markRefunded(now);
        long amountInCents = ticket.getOrderItem().getUnitPriceInCents();
        var result = refundGateway.refund(new RefundRequest(ticket.getId(), amountInCents, source.name()));
        return refundRepository.save(new TicketRefund(
            ticket,
            ticket.getOrder(),
            ticket.getOrder().getEvent(),
            actor,
            source,
            org.example.ingresso.ingresso.model.enums.TicketRefundStatus.valueOf(result.status()),
            amountInCents,
            reason,
            now
        ));
    }

    private TicketRefundView response(TicketRefund refund) {
        return new TicketRefundView(
            refund.getId(), refund.getTicket().getId(), refund.getOrder().getId(), refund.getEvent().getId(),
            refund.getSource().name(), refund.getStatus().name(), refund.getAmountInCents(), refund.getReason(),
            refund.getCreatedAt()
        );
    }

    private String normalizeReason(String reason) {
        if (reason == null || reason.isBlank()) return "Solicitação do comprador";
        return reason.trim();
    }
}