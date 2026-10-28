package org.example.ingresso.ingresso.controller;

import org.example.ingresso.ingresso.dto.AdminFinancialReport;
import org.example.ingresso.ingresso.dto.EventFinancialReport;
import org.example.ingresso.ingresso.service.FinancialReportService;
import org.example.ingresso.ingresso.service.dto.CoreServiceContracts.FinancialReport;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.example.ingresso.ingresso.config.Constants.SECURITY_ROLE_ADMIN;

@RestController
@RequestMapping("/api/admin/financeiro")
@PreAuthorize(SECURITY_ROLE_ADMIN)
public class AdminFinanceController {

    private final FinancialReportService reportService;

    public AdminFinanceController(FinancialReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping
    public ResponseEntity<AdminFinancialReport> report() {
        FinancialReport report = reportService.administratorReport();
        return ResponseEntity.ok(new AdminFinancialReport(
            report.paidOrderCount(), report.paidTicketCount(), report.refundedTicketCount(),
            report.grossSalesInCents(), report.simulatedRefundsInCents(), report.remainingGrossInCents(),
            report.events().stream().map(event -> new EventFinancialReport(
                event.eventId(), event.eventTitle(), event.paidOrderCount(), event.paidTicketCount(),
                event.refundedTicketCount(), event.grossSalesInCents(), event.simulatedRefundsInCents(),
                event.remainingGrossInCents()
            )).toList()
        ));
    }
}