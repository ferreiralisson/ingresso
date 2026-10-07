package org.example.ingresso.ingresso.model;

import org.example.ingresso.ingresso.model.enums.EventStatus;
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
@Table(name = "events")
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "producer_id", nullable = false)
    private Usuario producer;

    @Column(nullable = false, length = 160)
    private String title;

    @Column(nullable = false, length = 5000)
    private String description;

    @Column(name = "image_url", nullable = false, length = 2048)
    private String imageUrl;

    @Column(name = "starts_at", nullable = false)
    private Instant startsAt;

    @Column(name = "venue_name", nullable = false, length = 200)
    private String venueName;

    @Column(name = "street_address", nullable = false, length = 300)
    private String streetAddress;

    @Column(nullable = false, length = 120)
    private String city;

    @Column(name = "state_code", nullable = false, length = 2)
    private String stateCode;

    @Column(name = "age_classification", nullable = false, length = 80)
    private String ageClassification;

    @Column(nullable = false, length = 200)
    private String organizer;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EventStatus status;

    @Version
    @Column(nullable = false)
    private Long version;

    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("code ASC")
    private List<TicketCategory> categories = new ArrayList<>();

    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("positionIndex ASC")
    private List<SeatSector> sectors = new ArrayList<>();

    protected Event() {
    }

    public Event(
        Usuario producer,
        String title,
        String description,
        String imageUrl,
        Instant startsAt,
        String venueName,
        String streetAddress,
        String city,
        String stateCode,
        String ageClassification,
        String organizer
    ) {
        this.producer = producer;
        this.title = title;
        this.description = description;
        this.imageUrl = imageUrl;
        this.startsAt = startsAt;
        this.venueName = venueName;
        this.streetAddress = streetAddress;
        this.city = city;
        this.stateCode = stateCode;
        this.ageClassification = ageClassification;
        this.organizer = organizer;
        this.status = EventStatus.PUBLISHED;
    }

    public void updateDetails(
        String title,
        String description,
        String imageUrl,
        Instant startsAt,
        String venueName,
        String streetAddress,
        String city,
        String stateCode,
        String ageClassification,
        String organizer
    ) {
        this.title = title;
        this.description = description;
        this.imageUrl = imageUrl;
        this.startsAt = startsAt;
        this.venueName = venueName;
        this.streetAddress = streetAddress;
        this.city = city;
        this.stateCode = stateCode;
        this.ageClassification = ageClassification;
        this.organizer = organizer;
    }

    public void addCategory(TicketCategory category) {
        categories.add(category);
        category.setEvent(this);
    }

    public void removeCategory(TicketCategory category) {
        categories.remove(category);
        category.setEvent(null);
    }

    public void addSector(SeatSector sector) {
        sectors.add(sector);
        sector.setEvent(this);
    }

    public void clearSeatMap() {
        sectors.clear();
    }

    public void suspend() {
        if (status != EventStatus.PUBLISHED) {
            throw new IllegalArgumentException("Somente eventos publicados podem ser suspensos");
        }
        status = EventStatus.SUSPENDED;
    }

    public void reactivate() {
        if (status != EventStatus.SUSPENDED) {
            throw new IllegalArgumentException("Somente eventos suspensos podem ser reativados");
        }
        status = EventStatus.PUBLISHED;
    }

    public void cancel() {
        if (status == EventStatus.CANCELLED) {
            throw new IllegalArgumentException("Evento ja cancelado");
        }
        status = EventStatus.CANCELLED;
    }

    public Long getId() {
        return id;
    }

    public Usuario getProducer() {
        return producer;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public Instant getStartsAt() {
        return startsAt;
    }

    public String getVenueName() {
        return venueName;
    }

    public String getStreetAddress() {
        return streetAddress;
    }

    public String getCity() {
        return city;
    }

    public String getStateCode() {
        return stateCode;
    }

    public String getAgeClassification() {
        return ageClassification;
    }

    public String getOrganizer() {
        return organizer;
    }

    public EventStatus getStatus() {
        return status;
    }

    public List<TicketCategory> getCategories() {
        return Collections.unmodifiableList(categories);
    }

    public List<SeatSector> getSectors() {
        return Collections.unmodifiableList(sectors);
    }
}