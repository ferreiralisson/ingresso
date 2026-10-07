package org.example.ingresso.ingresso.model;

import org.example.ingresso.ingresso.model.enums.TicketRefundSource;
import org.example.ingresso.ingresso.model.enums.TicketRefundStatus;
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
@Table(name = "ticket_refunds")
public class TicketRefund {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticket_id", nullable = false, unique = true)
    private IssuedTicket ticket;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private PurchaseOrder order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_user_id", nullable = false)
    private Usuario actor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TicketRefundSource source;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TicketRefundStatus status;

    @Column(name = "amount_in_cents", nullable = false)
    private long amountInCents;

    @Column(length = 500)
    private String reason;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected TicketRefund() {
    }

    public TicketRefund(
        IssuedTicket ticket,
        PurchaseOrder order,
        Event event,
        Usuario actor,
        TicketRefundSource source,
        TicketRefundStatus status,
        long amountInCents,
        String reason,
        Instant createdAt
    ) {
        this.ticket = ticket;
        this.order = order;
        this.event = event;
        this.actor = actor;
        this.source = source;
        this.status = status;
        this.amountInCents = amountInCents;
        this.reason = reason;
        this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public IssuedTicket getTicket() { return ticket; }
    public PurchaseOrder getOrder() { return order; }
    public Event getEvent() { return event; }
    public Usuario getActor() { return actor; }
    public TicketRefundSource getSource() { return source; }
    public TicketRefundStatus getStatus() { return status; }
    public long getAmountInCents() { return amountInCents; }
    public String getReason() { return reason; }
    public Instant getCreatedAt() { return createdAt; }
}