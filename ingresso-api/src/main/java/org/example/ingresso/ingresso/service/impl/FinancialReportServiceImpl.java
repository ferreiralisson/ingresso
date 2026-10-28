package org.example.ingresso.ingresso.service.impl;

import org.example.ingresso.ingresso.service.FinancialReportService;
import org.example.ingresso.ingresso.service.dto.CoreServiceContracts.FinancialEventReport;
import org.example.ingresso.ingresso.service.dto.CoreServiceContracts.FinancialReport;
import org.example.ingresso.ingresso.model.Event;
import org.example.ingresso.ingresso.model.IssuedTicket;
import org.example.ingresso.ingresso.model.enums.OrderStatus;
import org.example.ingresso.ingresso.repository.EventRepository;
import org.example.ingresso.ingresso.repository.IssuedTicketRepository;
import org.example.ingresso.ingresso.repository.TicketRefundRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.example.ingresso.ingresso.service.exception.ApplicationServiceException;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class FinancialReportServiceImpl implements FinancialReportService {

    private final EventRepository eventRepository;
    private final IssuedTicketRepository ticketRepository;
    private final TicketRefundRepository refundRepository;

    public FinancialReportServiceImpl(
        EventRepository eventRepository,
        IssuedTicketRepository ticketRepository,
        TicketRefundRepository refundRepository
    ) {
        this.eventRepository = eventRepository;
        this.ticketRepository = ticketRepository;
        this.refundRepository = refundRepository;
    }

    @Transactional(readOnly = true)
    public FinancialEventReport producerEventReport(Long eventId, Long producerId) {
        Event event = eventRepository.findByIdAndProducer_Id(eventId, producerId)
            .orElseThrow(() -> new ApplicationServiceException(
                ApplicationServiceException.Code.NOT_FOUND, "Evento nao encontrado"
            ));
        return report(event);
    }

    @Transactional(readOnly = true)
    public FinancialReport administratorReport() {
        List<FinancialEventReport> reports = eventRepository.findAll(Sort.by("id").ascending()).stream()
            .map(this::report)
            .toList();
        return new FinancialReport(
            reports.stream().mapToLong(FinancialEventReport::paidOrderCount).sum(),
            reports.stream().mapToLong(FinancialEventReport::paidTicketCount).sum(),
            reports.stream().mapToLong(FinancialEventReport::refundedTicketCount).sum(),
            reports.stream().mapToLong(FinancialEventReport::grossSalesInCents).sum(),
            reports.stream().mapToLong(FinancialEventReport::simulatedRefundsInCents).sum(),
            reports.stream().mapToLong(FinancialEventReport::remainingGrossInCents).sum(),
            reports
        );
    }

    private FinancialEventReport report(Event event) {
        List<IssuedTicket> paidTickets = ticketRepository.findByOrder_Event_IdOrderByIdAsc(event.getId()).stream()
            .filter(ticket -> ticket.getOrder().getStatus() == OrderStatus.PAID)
            .toList();
        Set<Long> paidOrderIds = paidTickets.stream().map(ticket -> ticket.getOrder().getId()).collect(Collectors.toSet());
        var refunds = refundRepository.findByEvent_Id(event.getId());
        long gross = paidTickets.stream().mapToLong(ticket -> ticket.getOrderItem().getUnitPriceInCents()).sum();
        long refunded = refunds.stream().mapToLong(refund -> refund.getAmountInCents()).sum();
        return new FinancialEventReport(
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