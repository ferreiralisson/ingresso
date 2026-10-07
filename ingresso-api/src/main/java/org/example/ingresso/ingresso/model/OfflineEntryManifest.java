package org.example.ingresso.ingresso.model;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "offline_entry_manifests")
public class OfflineEntryManifest {

    @Id
    @Column(length = 36)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "operator_user_id", nullable = false)
    private Usuario operator;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @ElementCollection
    @CollectionTable(name = "offline_entry_manifest_tickets", joinColumns = @JoinColumn(name = "manifest_id"))
    @Column(name = "qr_token_hash", nullable = false, length = 64)
    private Set<String> ticketHashes = new LinkedHashSet<>();

    protected OfflineEntryManifest() {
    }

    public OfflineEntryManifest(String id, Event event, Usuario operator, Instant createdAt, Instant expiresAt, Set<String> ticketHashes) {
        this.id = id;
        this.event = event;
        this.operator = operator;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
        this.ticketHashes = new LinkedHashSet<>(ticketHashes);
    }

    public String getId() { return id; }
    public Event getEvent() { return event; }
    public Usuario getOperator() { return operator; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getExpiresAt() { return expiresAt; }
    public Set<String> getTicketHashes() { return Set.copyOf(ticketHashes); }
}