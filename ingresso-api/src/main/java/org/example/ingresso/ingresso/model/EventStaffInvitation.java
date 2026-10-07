package org.example.ingresso.ingresso.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "event_staff_invitations")
public class EventStaffInvitation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @Column(nullable = false, length = 320)
    private String email;

    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invited_by_user_id", nullable = false)
    private Usuario invitedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "accepted_by_user_id")
    private Usuario acceptedBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "accepted_at")
    private Instant acceptedAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    protected EventStaffInvitation() {
    }

    public EventStaffInvitation(Event event, String email, String tokenHash, Usuario invitedBy, Instant createdAt, Instant expiresAt) {
        this.event = event;
        this.email = email;
        this.tokenHash = tokenHash;
        this.invitedBy = invitedBy;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
    }

    public void accept(Usuario user, Instant at) {
        if (acceptedAt != null || revokedAt != null || !expiresAt.isAfter(at)) {
            throw new IllegalStateException("Convite de equipe nao pode ser aceito");
        }
        acceptedBy = user;
        acceptedAt = at;
    }

    public void revoke(Instant at) {
        if (revokedAt != null) {
            throw new IllegalStateException("Convite ou permissao ja revogado");
        }
        revokedAt = at;
    }

    public boolean isAcceptedAndActive() {
        return acceptedAt != null && revokedAt == null;
    }

    public Long getId() { return id; }
    public Event getEvent() { return event; }
    public String getEmail() { return email; }
    public String getTokenHash() { return tokenHash; }
    public Usuario getInvitedBy() { return invitedBy; }
    public Usuario getAcceptedBy() { return acceptedBy; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getExpiresAt() { return expiresAt; }
    public Instant getAcceptedAt() { return acceptedAt; }
    public Instant getRevokedAt() { return revokedAt; }
}