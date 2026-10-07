package org.example.ingresso.ingresso.service;

import org.example.ingresso.ingresso.dto.AdminFinancialReport;
import org.example.ingresso.ingresso.dto.EventFinancialReport;
import org.example.ingresso.ingresso.model.Event;
import org.example.ingresso.ingresso.model.IssuedTicket;
import org.example.ingresso.ingresso.model.enums.OrderStatus;
import org.example.ingresso.ingresso.repository.EventRepository;
import org.example.ingresso.ingresso.repository.IssuedTicketRepository;
import org.example.ingresso.ingresso.repository.TicketRefundRepository;
import org.example.ingresso.ingresso.security.UserSS;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class FinancialReportService {

    private final EventRepository eventRepository;
    private final IssuedTicketRepository ticketRepository;
    private final TicketRefundRepository refundRepository;

    public FinancialReportService(
        EventRepository eventRepository,
        IssuedTicketRepository ticketRepository,
        TicketRefundRepository refundRepository
    ) {
        this.eventRepository = eventRepository;
        this.ticketRepository = ticketRepository;
        this.refundRepository = refundRepository;
    }

    @Transactional(readOnly = true)
    public EventFinancialReport producerEventReport(Long eventId, UserSS producer) {
        Event event = eventRepository.findByIdAndProducer_Id(eventId, producer.getId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        return report(event);
    }

    @Transactional(readOnly = true)
    public AdminFinancialReport administratorReport() {
        List<EventFinancialReport> reports = eventRepository.findAll(Sort.by("id").ascending()).stream()
            .map(this::report)
            .toList();
        return new AdminFinancialReport(
            reports.stream().mapToLong(EventFinancialReport::paidOrderCount).sum(),
            reports.stream().mapToLong(EventFinancialReport::paidTicketCount).sum(),
            reports.stream().mapToLong(EventFinancialReport::refundedTicketCount).sum(),
            reports.stream().mapToLong(EventFinancialReport::grossSalesInCents).sum(),
            reports.stream().mapToLong(EventFinancialReport::simulatedRefundsInCents).sum(),
            reports.stream().mapToLong(EventFinancialReport::remainingGrossInCents).sum(),
            reports
        );
    }

    private EventFinancialReport report(Event event) {
        List<IssuedTicket> paidTickets = ticketRepository.findByOrder_Event_IdOrderByIdAsc(event.getId()).stream()
            .filter(ticket -> ticket.getOrder().getStatus() == OrderStatus.PAID)
            .toList();
        Set<Long> paidOrderIds = paidTickets.stream().map(ticket -> ticket.getOrder().getId()).collect(Collectors.toSet());
        var refunds = refundRepository.findByEvent_Id(event.getId());
        long gross = paidTickets.stream().mapToLong(ticket -> ticket.getOrderItem().getUnitPriceInCents()).sum();
        long refunded = refunds.stream().mapToLong(refund -> refund.getAmountInCents()).sum();
        return new EventFinancialReport(
            event.getId(),
            event.getTitle(),
            paidOrderIds.size(),
            paidTickets.size(),
            refunds.size(),
            gross,
            refunded,
            Math.max(0, gross - refunded)
        );
    }
}