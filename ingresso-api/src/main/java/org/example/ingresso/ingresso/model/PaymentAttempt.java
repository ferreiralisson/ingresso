package org.example.ingresso.ingresso.model;

import org.example.ingresso.ingresso.model.enums.PaymentOutcome;
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
@Table(name = "payment_attempts")
public class PaymentAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private PurchaseOrder order;

    @Column(name = "attempt_key", nullable = false, unique = true, length = 64)
    private String attemptKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentOutcome outcome;

    @Column(nullable = false, length = 20)
    private String provider;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    protected PaymentAttempt() {
    }

    public PaymentAttempt(String attemptKey, PaymentOutcome outcome, String provider, Instant startedAt, Instant completedAt) {
        this.attemptKey = attemptKey;
        this.outcome = outcome;
        this.provider = provider;
        this.startedAt = startedAt;
        this.completedAt = completedAt;
    }

    void setOrder(PurchaseOrder order) {
        this.order = order;
    }

    public Long getId() {
        return id;
    }

    public PurchaseOrder getOrder() {
        return order;
    }

    public String getAttemptKey() {
        return attemptKey;
    }

    public PaymentOutcome getOutcome() {
        return outcome;
    }

    public String getProvider() {
        return provider;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void complete(PaymentOutcome result, Instant completedAt) {
        this.outcome = result;
        this.completedAt = completedAt;
    }
}