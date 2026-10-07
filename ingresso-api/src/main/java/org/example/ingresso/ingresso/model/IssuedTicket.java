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
@Table(name = "issued_tickets")
public class IssuedTicket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private PurchaseOrder order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_item_id", nullable = false)
    private OrderItem orderItem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "used_by_user_id")
    private Usuario usedBy;

    @Column(name = "unit_number", nullable = false)
    private int unitNumber;

    @Column(name = "seat_event_id", unique = true)
    private Long eventSeatId;

    @Column(name = "sector_snapshot", length = 100)
    private String sector;

    @Column(name = "row_snapshot", length = 20)
    private String row;

    @Column(name = "seat_label_snapshot", length = 20)
    private String seatLabel;

    @Column(name = "qr_token", nullable = false, unique = true, length = 64)
    private String qrToken;

    @Column(name = "qr_token_hash", nullable = false, unique = true, length = 64)
    private String qrTokenHash;

    @Column(name = "issued_at", nullable = false)
    private Instant issuedAt;

    @Column(name = "used_at")
    private Instant usedAt;

    @Column(name = "refunded_at")
    private Instant refundedAt;

    @Column(name = "correction_used", nullable = false)
    private boolean correctionUsed;

    protected IssuedTicket() {
    }

    public IssuedTicket(
        PurchaseOrder order,
        OrderItem orderItem,
        int unitNumber,
        OrderSeatSnapshot seat,
        String qrToken,
        String qrTokenHash,
        Instant issuedAt
    ) {
        this.order = order;
        this.orderItem = orderItem;
        this.unitNumber = unitNumber;
        this.eventSeatId = seat == null ? null : seat.getEventSeatId();
        this.sector = seat == null ? null : seat.getSectorName();
        this.row = seat == null ? null : seat.getRowLabel();
        this.seatLabel = seat == null ? null : seat.getSeatLabel();
        this.qrToken = qrToken;
        this.qrTokenHash = qrTokenHash;
        this.issuedAt = issuedAt;
    }

    public Long getId() {
        return id;
    }

    public PurchaseOrder getOrder() {
        return order;
    }

    public OrderItem getOrderItem() {
        return orderItem;
    }

    public int getUnitNumber() {
        return unitNumber;
    }

    public Long getEventSeatId() {
        return eventSeatId;
    }

    public String getSector() {
        return sector;
    }

    public String getRow() {
        return row;
    }

    public String getSeatLabel() {
        return seatLabel;
    }

    public String getQrToken() {
        return qrToken;
    }

    public String getQrTokenHash() {
        return qrTokenHash;
    }

    public Instant getIssuedAt() {
        return issuedAt;
    }

    public Usuario getUsedBy() {
        return usedBy;
    }

    public Instant getUsedAt() {
        return usedAt;
    }

    public Instant getRefundedAt() {
        return refundedAt;
    }

    public boolean isRefunded() {
        return refundedAt != null;
    }

    public boolean isCorrectionUsed() {
        return correctionUsed;
    }

    public boolean isUsed() {
        return usedAt != null;
    }

    public void consume(Usuario operator, Instant at) {
        if (isUsed()) {
            throw new IllegalStateException("Ingresso ja utilizado");
        }
        usedBy = operator;
        usedAt = at;
    }

    public void correctUsage() {
        if (!isUsed() || correctionUsed) {
            throw new IllegalStateException("Uso nao pode ser corrigido");
        }
        usedBy = null;
        usedAt = null;
        correctionUsed = true;
    }

    public void markRefunded(Instant at) {
        if (isUsed() || isRefunded()) {
            throw new IllegalStateException("Ingresso usado ou ja reembolsado");
        }
        refundedAt = at;
    }
}