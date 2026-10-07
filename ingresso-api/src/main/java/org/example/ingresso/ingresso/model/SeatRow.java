package org.example.ingresso.ingresso.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Entity
@Table(name = "seat_rows")
public class SeatRow {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sector_id", nullable = false)
    private SeatSector sector;

    @Column(nullable = false, length = 20)
    private String label;

    @Column(name = "position_index", nullable = false)
    private int positionIndex;

    @OneToMany(mappedBy = "row", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("positionIndex ASC")
    private List<EventSeat> seats = new ArrayList<>();

    protected SeatRow() {
    }

    public SeatRow(String label, int positionIndex) {
        this.label = label;
        this.positionIndex = positionIndex;
    }

    void setSector(SeatSector sector) {
        this.sector = sector;
    }

    public void addSeat(EventSeat seat) {
        seats.add(seat);
        seat.setRow(this);
    }

    public Long getId() {
        return id;
    }

    public SeatSector getSector() {
        return sector;
    }

    public String getLabel() {
        return label;
    }

    public int getPositionIndex() {
        return positionIndex;
    }

    public List<EventSeat> getSeats() {
        return Collections.unmodifiableList(seats);
    }
}