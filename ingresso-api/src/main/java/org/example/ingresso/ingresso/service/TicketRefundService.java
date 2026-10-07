package org.example.ingresso.ingresso.service;

import org.example.ingresso.ingresso.dto.TicketRefundResponse;
import org.example.ingresso.ingresso.model.Event;
import org.example.ingresso.ingresso.model.IssuedTicket;
import org.example.ingresso.ingresso.model.TicketRefund;
import org.example.ingresso.ingresso.model.Usuario;
import org.example.ingresso.ingresso.model.enums.OrderStatus;
import org.example.ingresso.ingresso.model.enums.TicketRefundSource;
import org.example.ingresso.ingresso.repository.IssuedTicketRepository;
import org.example.ingresso.ingresso.repository.TicketRefundRepository;
import org.example.ingresso.ingresso.repository.UsuarioRepository;
import org.example.ingresso.ingresso.security.UserSS;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
public class TicketRefundService {

    private final IssuedTicketRepository ticketRepository;
    private final TicketRefundRepository refundRepository;
    private final UsuarioRepository usuarioRepository;
    private final RefundGateway refundGateway;

    public TicketRefundService(
        IssuedTicketRepository ticketRepository,
        TicketRefundRepository refundRepository,
        UsuarioRepository usuarioRepository,
        RefundGateway refundGateway
    ) {
        this.ticketRepository = ticketRepository;
        this.refundRepository = refundRepository;
        this.usuarioRepository = usuarioRepository;
        this.refundGateway = refundGateway;
    }

    @Transactional
    public TicketRefundResponse requestBuyerRefund(Long orderId, Long ticketId, String reason, UserSS principal) {
        IssuedTicket ticket = ticketRepository.findWithLockById(ticketId)
            .orElseThrow(() -> new IllegalArgumentException("Ingresso nao encontrado"));
        if (!ticket.getOrder().getId().equals(orderId)
            || !ticket.getOrder().getBuyer().getId().equals(principal.getId())) {
            throw new AccessDeniedException("Ingresso nao pertence ao comprador autenticado");
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

        Usuario buyer = usuarioRepository.findWithPerfisById(principal.getId())
            .orElseThrow(() -> new AccessDeniedException("Comprador autenticado nao encontrado"));
        return response(createRefund(ticket, buyer, TicketRefundSource.BUYER_REQUEST,
            normalizeReason(reason), now));
    }

    @Transactional
    public List<TicketRefundResponse> refundUnusedTicketsForEvent(Event event, Usuario actor) {
        List<IssuedTicket> tickets = ticketRepository.findByOrder_Event_IdOrderByIdAsc(event.getId());
        List<TicketRefundResponse> refunds = new ArrayList<>();
        for (IssuedTicket candidate : tickets) {
            IssuedTicket ticket = ticketRepository.findWithLockById(candidate.getId()).orElse(null);
            if (ticket == null || ticket.getOrder().getStatus() != OrderStatus.PAID
                || ticket.isUsed() || ticket.isRefunded()
                || refundRepository.findByTicket_Id(ticket.getId()).isPresent()) {
                continue;
            }
            refunds.add(response(createRefund(
                ticket, actor, TicketRefundSource.EVENT_CANCELLATION, "Evento cancelado", Instant.now()
            )));
        }
        return List.copyOf(refunds);
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
        var status = refundGateway.refund(ticket.getId(), amountInCents, source);
        return refundRepository.save(new TicketRefund(
            ticket,
            ticket.getOrder(),
            ticket.getOrder().getEvent(),
            actor,
            source,
            status,
            amountInCents,
            reason,
            now
        ));
    }

    private TicketRefundResponse response(TicketRefund refund) {
        return new TicketRefundResponse(
            refund.getId(), refund.getTicket().getId(), refund.getOrder().getId(), refund.getEvent().getId(),
            refund.getSource(), refund.getStatus(), refund.getAmountInCents(), refund.getReason(), refund.getCreatedAt()
        );
    }

    private String normalizeReason(String reason) {
        if (reason == null || reason.isBlank()) return "Solicitação do comprador";
        return reason.trim();
    }
}