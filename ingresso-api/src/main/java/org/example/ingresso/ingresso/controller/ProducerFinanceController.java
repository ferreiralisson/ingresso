package org.example.ingresso.ingresso.controller;

import org.example.ingresso.ingresso.dto.EventFinancialReport;
import org.example.ingresso.ingresso.security.UserSS;
import org.example.ingresso.ingresso.service.FinancialReportService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.example.ingresso.ingresso.config.Constants.SECURITY_ROLE_PRODUCER;

@RestController
@RequestMapping("/api/produtor/eventos")
@PreAuthorize(SECURITY_ROLE_PRODUCER)
public class ProducerFinanceController {

    private final FinancialReportService reportService;

    public ProducerFinanceController(FinancialReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/{eventId}/financeiro")
    public ResponseEntity<EventFinancialReport> eventReport(
        @PathVariable Long eventId,
        @AuthenticationPrincipal UserSS producer
    ) {
        return ResponseEntity.ok(reportService.producerEventReport(eventId, producer));
    }
}