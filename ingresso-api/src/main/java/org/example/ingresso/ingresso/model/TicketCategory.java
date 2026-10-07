package org.example.ingresso.ingresso.model;

import org.example.ingresso.ingresso.model.enums.AdmissionMode;
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
@Table(name = "ticket_categories")
public class TicketCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @Column(nullable = false, length = 40)
    private String code;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(name = "price_in_cents", nullable = false)
    private long priceInCents;

    @Enumerated(EnumType.STRING)
    @Column(name = "admission_mode", nullable = false, length = 30)
    private AdmissionMode admissionMode;

    @Column(name = "configured_quantity")
    private Integer configuredQuantity;

    @Column(name = "sold_quantity", nullable = false)
    private int soldQuantity;

    @Column(name = "reserved_quantity", nullable = false)
    private int reservedQuantity;

    protected TicketCategory() {
    }

    public TicketCategory(String code, String name, long priceInCents, AdmissionMode admissionMode, Integer configuredQuantity) {
        this.code = code;
        this.name = name;
        this.priceInCents = priceInCents;
        this.admissionMode = admissionMode;
        this.configuredQuantity = configuredQuantity;
    }

    void setEvent(Event event) {
        this.event = event;
    }

    public void update(String name, long priceInCents, AdmissionMode admissionMode, Integer configuredQuantity) {
        this.name = name;
        this.priceInCents = priceInCents;
        this.admissionMode = admissionMode;
        this.configuredQuantity = configuredQuantity;
    }

    public int getAvailableQuantity() {
        if (admissionMode == AdmissionMode.ASSIGNED_SEAT) {
            return 0;
        }
        return Math.max(0, configuredQuantity - soldQuantity - reservedQuantity);
    }

    public void reserve(int quantity) {
        if (quantity < 1 || getAvailableQuantity() < quantity) {
            throw new IllegalArgumentException("Estoque insuficiente");
        }
        reservedQuantity += quantity;
    }

    public void reserveAssignedSeats(int quantity) {
        if (admissionMode != AdmissionMode.ASSIGNED_SEAT || quantity < 1) {
            throw new IllegalArgumentException("Reserva de assentos invalida");
        }
        reservedQuantity += quantity;
    }

    public void releaseReservation(int quantity) {
        if (quantity < 1 || reservedQuantity < quantity) {
            throw new IllegalStateException("Reserva de estoque inconsistente");
        }
        reservedQuantity -= quantity;
    }

    public void commitReservation(int quantity) {
        releaseReservation(quantity);
        soldQuantity += quantity;
    }

    public Long getId() {
        return id;
    }

    public Event getEvent() {
        return event;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public long getPriceInCents() {
        return priceInCents;
    }

    public AdmissionMode getAdmissionMode() {
        return admissionMode;
    }

    public Integer getConfiguredQuantity() {
        return configuredQuantity;
    }

    public int getSoldQuantity() {
        return soldQuantity;
    }

    public int getReservedQuantity() {
        return reservedQuantity;
    }
}