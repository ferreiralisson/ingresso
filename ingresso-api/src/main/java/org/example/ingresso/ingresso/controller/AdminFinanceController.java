package org.example.ingresso.ingresso.controller;

import org.example.ingresso.ingresso.dto.AdminFinancialReport;
import org.example.ingresso.ingresso.service.FinancialReportService;
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
        return ResponseEntity.ok(reportService.administratorReport());
    }
}