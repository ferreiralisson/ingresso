package org.example.ingresso.ingresso.service.dto;

import java.time.Instant;
import java.util.List;

public final class EntryContracts {

    private EntryContracts() {
    }

    public record EntryValidationCommand(String scanId, String qrCodeValue) {
    }

    public record EntryCorrectionCommand(String reason) {
    }

    public record OfflineScanCommand(String scanId, String qrTokenHash, Instant scannedAt) {
    }

    public record OfflineSyncCommand(String manifestId, String deviceId, List<OfflineScanCommand> scans) {
        public OfflineSyncCommand {
            scans = List.copyOf(scans);
        }
    }

    public record EntryValidationView(
        String outcome,
        String message,
        Long ticketId,
        Long checkInId,
        Instant processedAt,
        Instant previousUseAt,
        String previousOperator,
        String source,
        Instant deviceScannedAt,
        Instant synchronizedAt,
        String operatorEmail,
        Instant correctedAt,
        String correctionReason,
        String correctedBy
    ) {
    }

    public record OfflineManifestView(
        String id,
        Long eventId,
        String operatorEmail,
        String eventTitle,
        Instant eventStartsAt,
        Instant createdAt,
        Instant expiresAt,
        List<Ticket> tickets
    ) {
        public OfflineManifestView {
            tickets = List.copyOf(tickets);
        }

        public record Ticket(
            Long id,
            String qrTokenHash,
            boolean used,
            String categoryName,
            int unitNumber,
            String sector,
            String row,
            String seatLabel
        ) {
        }
    }

    public record OfflineSyncView(String manifestId, Instant synchronizedAt, List<EntryValidationView> results) {
        public OfflineSyncView {
            results = List.copyOf(results);
        }
    }
}