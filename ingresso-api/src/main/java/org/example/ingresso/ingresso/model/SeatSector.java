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
@Table(name = "seat_sectors")
public class SeatSector {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "position_index", nullable = false)
    private int positionIndex;

    @OneToMany(mappedBy = "sector", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("positionIndex ASC")
    private List<SeatRow> rows = new ArrayList<>();

    protected SeatSector() {
    }

    public SeatSector(String name, int positionIndex) {
        this.name = name;
        this.positionIndex = positionIndex;
    }

    void setEvent(Event event) {
        this.event = event;
    }

    public void addRow(SeatRow row) {
        rows.add(row);
        row.setSector(this);
    }

    public Long getId() {
        return id;
    }

    public Event getEvent() {
        return event;
    }

    public String getName() {
        return name;
    }

    public int getPositionIndex() {
        return positionIndex;
    }

    public List<SeatRow> getRows() {
        return Collections.unmodifiableList(rows);
    }
}