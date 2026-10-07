package org.example.ingresso.ingresso.model;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Entity
@Table(name = "order_items")
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private PurchaseOrder order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticket_category_id", nullable = false)
    private TicketCategory category;

    @Column(name = "category_code_snapshot", nullable = false, length = 40)
    private String categoryCodeSnapshot;

    @Column(name = "category_name_snapshot", nullable = false, length = 120)
    private String categoryNameSnapshot;

    @Column(name = "admission_mode_snapshot", nullable = false, length = 30)
    private String admissionModeSnapshot;

    @Column(name = "unit_price_in_cents", nullable = false)
    private long unitPriceInCents;

    @Column(nullable = false)
    private int quantity;

    @ElementCollection
    @CollectionTable(name = "order_item_seats", joinColumns = @JoinColumn(name = "order_item_id"))
    @OrderBy("sectorName ASC, rowLabel ASC, seatLabel ASC")
    private List<OrderSeatSnapshot> seats = new ArrayList<>();

    protected OrderItem() {
    }

    public OrderItem(PurchaseOrder order, TicketCategory category, int quantity) {
        this.order = order;
        this.category = category;
        this.categoryCodeSnapshot = category.getCode();
        this.categoryNameSnapshot = category.getName();
        this.admissionModeSnapshot = category.getAdmissionMode().name();
        this.unitPriceInCents = category.getPriceInCents();
        this.quantity = quantity;
    }

    public void addSeatSnapshot(EventSeat seat) {
        SeatRow row = seat.getRow();
        seats.add(new OrderSeatSnapshot(
            seat.getId(),
            row.getSector().getName(),
            row.getLabel(),
            seat.getLabel()
        ));
    }

    public Long getId() {
        return id;
    }

    public PurchaseOrder getOrder() {
        return order;
    }

    public TicketCategory getCategory() {
        return category;
    }

    public String getCategoryCodeSnapshot() {
        return categoryCodeSnapshot;
    }

    public String getCategoryNameSnapshot() {
        return categoryNameSnapshot;
    }

    public String getAdmissionModeSnapshot() {
        return admissionModeSnapshot;
    }

    public long getUnitPriceInCents() {
        return unitPriceInCents;
    }

    public int getQuantity() {
        return quantity;
    }

    public List<OrderSeatSnapshot> getSeats() {
        return Collections.unmodifiableList(seats);
    }
}