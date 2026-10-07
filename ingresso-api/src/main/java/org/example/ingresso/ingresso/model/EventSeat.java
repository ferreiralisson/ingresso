package org.example.ingresso.ingresso.model;

import org.example.ingresso.ingresso.model.enums.SeatStatus;
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

@Entity
@Table(name = "event_seats")
public class EventSeat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "row_id", nullable = false)
    private SeatRow row;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticket_category_id", nullable = false)
    private TicketCategory category;

    @Column(nullable = false, length = 20)
    private String label;

    @Column(name = "position_index", nullable = false)
    private int positionIndex;

    @Column(name = "reserved_order_id")
    private Long reservedOrderId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SeatStatus status;

    protected EventSeat() {
    }

    public EventSeat(String label, int positionIndex, TicketCategory category) {
        this.label = label;
        this.positionIndex = positionIndex;
        this.category = category;
        this.status = SeatStatus.AVAILABLE;
    }

    void setRow(SeatRow row) {
        this.row = row;
    }

    public void reserve(Long orderId) {
        if (status != SeatStatus.AVAILABLE) {
            throw new IllegalArgumentException("Assento indisponivel");
        }
        status = SeatStatus.RESERVED;
        reservedOrderId = orderId;
    }

    public void release(Long orderId) {
        if (status != SeatStatus.RESERVED || !java.util.Objects.equals(reservedOrderId, orderId)) {
            throw new IllegalStateException("Reserva do assento inconsistente");
        }
        status = SeatStatus.AVAILABLE;
        reservedOrderId = null;
    }

    public void sell(Long orderId) {
        if (status != SeatStatus.RESERVED || !java.util.Objects.equals(reservedOrderId, orderId)) {
            throw new IllegalStateException("Reserva do assento inconsistente");
        }
        status = SeatStatus.SOLD;
    }

    public Long getId() {
        return id;
    }

    public SeatRow getRow() {
        return row;
    }

    public TicketCategory getCategory() {
        return category;
    }

    public String getLabel() {
        return label;
    }

    public int getPositionIndex() {
        return positionIndex;
    }

    public SeatStatus getStatus() {
        return status;
    }

    public Long getReservedOrderId() {
        return reservedOrderId;
    }
}