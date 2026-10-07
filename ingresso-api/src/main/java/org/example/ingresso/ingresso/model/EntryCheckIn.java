package org.example.ingresso.ingresso.model;

import org.example.ingresso.ingresso.model.enums.EntryCheckInOutcome;
import org.example.ingresso.ingresso.model.enums.EntryCheckInSource;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "entry_check_ins")
public class EntryCheckIn {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "issued_ticket_id")
    private IssuedTicket ticket;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "operator_user_id", nullable = false)
    private Usuario operator;

    @Column(name = "client_scan_id", nullable = false, length = 36)
    private String clientScanId;

    @Column(name = "qr_token_hash", length = 64)
    private String qrTokenHash;

    @Column(name = "device_id", length = 64)
    private String deviceId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "offline_manifest_id")
    private OfflineEntryManifest offlineManifest;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EntryCheckInSource source;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private EntryCheckInOutcome outcome;

    @Column(name = "device_scanned_at")
    private Instant deviceScannedAt;

    @Column(name = "synchronized_at")
    private Instant synchronizedAt;

    @Column(name = "processed_at", nullable = false)
    private Instant processedAt;

    protected EntryCheckIn() {
    }

    public EntryCheckIn(
        Event event,
        IssuedTicket ticket,
        Usuario operator,
        String clientScanId,
        String qrTokenHash,
        EntryCheckInSource source,
        EntryCheckInOutcome outcome,
        Instant deviceScannedAt,
        Instant synchronizedAt,
        Instant processedAt
    ) {
        this(event, ticket, operator, clientScanId, qrTokenHash, source, outcome, deviceScannedAt, synchronizedAt,
            processedAt, null, null);
    }

    public EntryCheckIn(
        Event event,
        IssuedTicket ticket,
        Usuario operator,
        String clientScanId,
        String qrTokenHash,
        EntryCheckInSource source,
        EntryCheckInOutcome outcome,
        Instant deviceScannedAt,
        Instant synchronizedAt,
        Instant processedAt,
        String deviceId,
        OfflineEntryManifest offlineManifest
    ) {
        this.event = event;
        this.ticket = ticket;
        this.operator = operator;
        this.clientScanId = clientScanId;
        this.qrTokenHash = qrTokenHash;
        this.source = source;
        this.outcome = outcome;
        this.deviceScannedAt = deviceScannedAt;
        this.synchronizedAt = synchronizedAt;
        this.processedAt = processedAt;
        this.deviceId = deviceId;
        this.offlineManifest = offlineManifest;
    }

    public Long getId() { return id; }
    public Event getEvent() { return event; }
    public IssuedTicket getTicket() { return ticket; }
    public Usuario getOperator() { return operator; }
    public String getClientScanId() { return clientScanId; }
    public EntryCheckInOutcome getOutcome() { return outcome; }
    public EntryCheckInSource getSource() { return source; }
    public Instant getDeviceScannedAt() { return deviceScannedAt; }
    public Instant getSynchronizedAt() { return synchronizedAt; }
    public Instant getProcessedAt() { return processedAt; }
    public String getDeviceId() { return deviceId; }
    public OfflineEntryManifest getOfflineManifest() { return offlineManifest; }
}