package org.example.ingresso.ingresso.dto;

import org.example.ingresso.ingresso.model.enums.AdmissionMode;
import org.example.ingresso.ingresso.model.enums.EventStatus;
import org.example.ingresso.ingresso.model.enums.SeatStatus;

import java.time.OffsetDateTime;
import java.util.List;

public record EventResponse(
    Long id,
    String title,
    String description,
    String imageUrl,
    OffsetDateTime startsAt,
    String timeZone,
    Venue venue,
    String ageClassification,
    String organizer,
    EventStatus status,
    List<Category> categories,
    List<Sector> sectors
) {
    public record Venue(String name, String streetAddress, String city, String stateCode) {
    }

    public record Category(
        Long id,
        String code,
        String name,
        long priceInCents,
        AdmissionMode admissionMode,
        int configuredQuantity,
        int availableQuantity,
        int soldQuantity
    ) {
    }

    public record Sector(Long id, String name, int positionIndex, List<Row> rows) {
    }

    public record Row(Long id, String label, int positionIndex, List<Seat> seats) {
    }

    public record Seat(
        Long id,
        String label,
        int positionIndex,
        String categoryCode,
        String categoryName,
        long priceInCents,
        SeatStatus status
    ) {
    }
}