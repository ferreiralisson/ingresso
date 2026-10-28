package org.example.ingresso.ingresso.service;

import org.example.ingresso.ingresso.service.dto.CoreServiceContracts.FinancialEventReport;
import org.example.ingresso.ingresso.service.dto.CoreServiceContracts.FinancialReport;

public interface FinancialReportService {

    FinancialEventReport producerEventReport(Long eventId, Long producerId);

    FinancialReport administratorReport();
}
