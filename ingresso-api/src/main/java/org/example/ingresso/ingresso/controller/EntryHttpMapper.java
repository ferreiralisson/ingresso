package org.example.ingresso.ingresso.controller;

import org.example.ingresso.ingresso.dto.EntryCorrectionRequest;
import org.example.ingresso.ingresso.dto.EntryValidationRequest;
import org.example.ingresso.ingresso.dto.EntryValidationResponse;
import org.example.ingresso.ingresso.dto.OfflineManifestResponse;
import org.example.ingresso.ingresso.dto.OfflineScanRequest;
import org.example.ingresso.ingresso.dto.OfflineSyncRequest;
import org.example.ingresso.ingresso.dto.OfflineSyncResponse;
import org.example.ingresso.ingresso.model.enums.EntryCheckInOutcome;
import org.example.ingresso.ingresso.model.enums.EntryCheckInSource;
import org.example.ingresso.ingresso.service.dto.EntryContracts.EntryCorrectionCommand;
import org.example.ingresso.ingresso.service.dto.EntryContracts.EntryValidationCommand;
import org.example.ingresso.ingresso.service.dto.EntryContracts.EntryValidationView;
import org.example.ingresso.ingresso.service.dto.EntryContracts.OfflineManifestView;
import org.example.ingresso.ingresso.service.dto.EntryContracts.OfflineScanCommand;
import org.example.ingresso.ingresso.service.dto.EntryContracts.OfflineSyncCommand;
import org.example.ingresso.ingresso.service.dto.EntryContracts.OfflineSyncView;

final class EntryHttpMapper {

    private EntryHttpMapper() {
    }

    static EntryValidationCommand toCommand(EntryValidationRequest request) {
        return new EntryValidationCommand(request.scanId(), request.qrCodeValue());
    }

    static EntryCorrectionCommand toCommand(EntryCorrectionRequest request) {
        return new EntryCorrectionCommand(request.reason());
    }

    static OfflineSyncCommand toCommand(OfflineSyncRequest request) {
        return new OfflineSyncCommand(
            request.manifestId(), request.deviceId(), request.scans().stream()
                .map(EntryHttpMapper::toCommand)
                .toList()
        );
    }

    private static OfflineScanCommand toCommand(OfflineScanRequest request) {
        return new OfflineScanCommand(request.scanId(), request.qrTokenHash(), request.scannedAt());
    }

    static EntryValidationResponse toResponse(EntryValidationView view) {
        return new EntryValidationResponse(
            EntryCheckInOutcome.valueOf(view.outcome()), view.message(), view.ticketId(), view.checkInId(),
            view.processedAt(), view.previousUseAt(), view.previousOperator(), EntryCheckInSource.valueOf(view.source()),
            view.deviceScannedAt(), view.synchronizedAt(), view.operatorEmail(), view.correctedAt(),
            view.correctionReason(), view.correctedBy()
        );
    }

    static OfflineManifestResponse toResponse(OfflineManifestView view) {
        return new OfflineManifestResponse(
            view.id(), view.eventId(), view.operatorEmail(), view.eventTitle(), view.eventStartsAt(),
            view.createdAt(), view.expiresAt(), view.tickets().stream()
                .map(ticket -> new OfflineManifestResponse.Ticket(
                    ticket.id(), ticket.qrTokenHash(), ticket.used(), ticket.categoryName(), ticket.unitNumber(),
                    ticket.sector(), ticket.row(), ticket.seatLabel()
                ))
                .toList()
        );
    }

    static OfflineSyncResponse toResponse(OfflineSyncView view) {
        return new OfflineSyncResponse(
            view.manifestId(), view.synchronizedAt(), view.results().stream()
                .map(EntryHttpMapper::toResponse)
                .toList()
        );
    }
}
