package org.example.ingresso.ingresso.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class OrderSeatSnapshot {

    @Column(name = "event_seat_id", nullable = false)
    private Long eventSeatId;

    @Column(name = "sector_name", nullable = false, length = 100)
    private String sectorName;

    @Column(name = "row_label", nullable = false, length = 20)
    private String rowLabel;

    @Column(name = "seat_label", nullable = false, length = 20)
    private String seatLabel;

    protected OrderSeatSnapshot() {
    }

    public OrderSeatSnapshot(Long eventSeatId, String sectorName, String rowLabel, String seatLabel) {
        this.eventSeatId = eventSeatId;
        this.sectorName = sectorName;
        this.rowLabel = rowLabel;
        this.seatLabel = seatLabel;
    }

    public Long getEventSeatId() {
        return eventSeatId;
    }

    public String getSectorName() {
        return sectorName;
    }

    public String getRowLabel() {
        return rowLabel;
    }

    public String getSeatLabel() {
        return seatLabel;
    }
}