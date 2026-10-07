package org.example.ingresso.ingresso.model;

import org.example.ingresso.ingresso.model.enums.ProducerInvitationStatus;
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
@Table(name = "producer_invitations")
public class ProducerInvitation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String email;

    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ProducerInvitationStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_user_id", nullable = false)
    private Usuario createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "accepted_by_user_id")
    private Usuario acceptedBy;

    @Column(name = "accepted_at")
    private Instant acceptedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "revoked_by_user_id")
    private Usuario revokedBy;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    protected ProducerInvitation() {
    }

    public ProducerInvitation(String email, String tokenHash, Usuario createdBy, Instant createdAt, Instant expiresAt) {
        this.email = email;
        this.tokenHash = tokenHash;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
        this.status = ProducerInvitationStatus.PENDING;
    }

    public void accept(Usuario user, Instant acceptedAt) {
        this.acceptedBy = user;
        this.acceptedAt = acceptedAt;
        this.status = ProducerInvitationStatus.ACCEPTED;
    }

    public void revoke(Usuario user, Instant revokedAt) {
        this.revokedBy = user;
        this.revokedAt = revokedAt;
        this.status = ProducerInvitationStatus.REVOKED;
    }

    public Long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public ProducerInvitationStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Usuario getCreatedBy() {
        return createdBy;
    }

    public Usuario getAcceptedBy() {
        return acceptedBy;
    }

    public Instant getAcceptedAt() {
        return acceptedAt;
    }

    public Usuario getRevokedBy() {
        return revokedBy;
    }

    public Instant getRevokedAt() {
        return revokedAt;
    }
}