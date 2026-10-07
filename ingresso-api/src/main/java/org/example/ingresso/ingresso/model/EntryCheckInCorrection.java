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
@Table(name = "entry_check_in_corrections")
public class EntryCheckInCorrection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "check_in_id", nullable = false)
    private EntryCheckIn checkIn;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticket_id", nullable = false)
    private IssuedTicket ticket;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "admin_user_id", nullable = false)
    private Usuario administrator;

    @Column(nullable = false, length = 500)
    private String reason;

    @Column(name = "corrected_at", nullable = false)
    private Instant correctedAt;

    protected EntryCheckInCorrection() {
    }

    public EntryCheckInCorrection(EntryCheckIn checkIn, IssuedTicket ticket, Usuario administrator, String reason, Instant correctedAt) {
        this.checkIn = checkIn;
        this.ticket = ticket;
        this.administrator = administrator;
        this.reason = reason;
        this.correctedAt = correctedAt;
    }

    public Long getId() { return id; }
    public EntryCheckIn getCheckIn() { return checkIn; }
    public IssuedTicket getTicket() { return ticket; }
    public Usuario getAdministrator() { return administrator; }
    public String getReason() { return reason; }
    public Instant getCorrectedAt() { return correctedAt; }
}