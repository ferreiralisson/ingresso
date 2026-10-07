package org.example.ingresso.ingresso.model;

import org.example.ingresso.ingresso.model.enums.OrderStatus;
import org.example.ingresso.ingresso.model.enums.PaymentOutcome;
import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Entity
@Table(name = "ticket_orders")
public class PurchaseOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "buyer_id", nullable = false)
    private Usuario buyer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @Column(name = "idempotency_key", nullable = false, length = 128)
    private String idempotencyKey;

    @Column(name = "request_payload_hash", nullable = false, length = 64)
    private String requestPayloadHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private OrderStatus status;

    @Column(name = "total_in_cents", nullable = false)
    private long totalInCents;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "reservation_expires_at", nullable = false)
    private Instant reservationExpiresAt;

    @Column(name = "paid_at")
    private Instant paidAt;

    @Column(name = "provider_reference", length = 128)
    private String providerReference;

    @Version
    @Column(nullable = false)
    private Long version;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private List<OrderItem> items = new ArrayList<>();

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("startedAt ASC")
    private List<PaymentAttempt> paymentAttempts = new ArrayList<>();

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private List<IssuedTicket> tickets = new ArrayList<>();

    protected PurchaseOrder() {
    }

    public PurchaseOrder(
        Usuario buyer,
        Event event,
        String idempotencyKey,
        String requestPayloadHash,
        Instant createdAt,
        Instant reservationExpiresAt
    ) {
        this.buyer = buyer;
        this.event = event;
        this.idempotencyKey = idempotencyKey;
        this.requestPayloadHash = requestPayloadHash;
        this.status = OrderStatus.PENDING_PAYMENT;
        this.totalInCents = 0;
        this.createdAt = createdAt;
        this.updatedAt = createdAt;
        this.reservationExpiresAt = reservationExpiresAt;
    }

    public void addItem(OrderItem item) {
        items.add(item);
        totalInCents = Math.addExact(totalInCents, Math.multiplyExact(item.getUnitPriceInCents(), item.getQuantity()));
    }

    public void addPaymentAttempt(PaymentAttempt attempt) {
        paymentAttempts.add(attempt);
        attempt.setOrder(this);
    }

    public void addTicket(IssuedTicket ticket) {
        tickets.add(ticket);
    }

    public boolean isPendingAt(Instant now) {
        return status == OrderStatus.PENDING_PAYMENT && reservationExpiresAt.isAfter(now);
    }

    public void applyPaymentOutcome(PaymentOutcome outcome, Instant now, String providerReference) {
        if (status != OrderStatus.PENDING_PAYMENT) {
            return;
        }
        this.updatedAt = now;
        this.providerReference = providerReference;
        if (outcome == PaymentOutcome.APPROVED) {
            if (!reservationExpiresAt.isAfter(now)) {
                status = OrderStatus.EXPIRED;
                return;
            }
            status = OrderStatus.PAID;
            paidAt = now;
        } else if (outcome == PaymentOutcome.DECLINED) {
            status = OrderStatus.PAYMENT_DECLINED;
        }
    }

    public void expire(Instant now) {
        if (status == OrderStatus.PENDING_PAYMENT) {
            status = OrderStatus.EXPIRED;
            updatedAt = now;
        }
    }

    public void cancelForEvent(Instant now) {
        if (status == OrderStatus.PENDING_PAYMENT) {
            status = OrderStatus.CANCELLED;
            updatedAt = now;
        }
    }

    public Long getId() {
        return id;
    }

    public Usuario getBuyer() {
        return buyer;
    }

    public Event getEvent() {
        return event;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public String getRequestPayloadHash() {
        return requestPayloadHash;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public long getTotalInCents() {
        return totalInCents;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Instant getReservationExpiresAt() {
        return reservationExpiresAt;
    }

    public Instant getPaidAt() {
        return paidAt;
    }

    public String getProviderReference() {
        return providerReference;
    }

    public List<OrderItem> getItems() {
        return Collections.unmodifiableList(items);
    }

    public List<PaymentAttempt> getPaymentAttempts() {
        return Collections.unmodifiableList(paymentAttempts);
    }

    public List<IssuedTicket> getTickets() {
        return Collections.unmodifiableList(tickets);
    }
}