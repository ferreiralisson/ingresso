package org.example.ingresso.ingresso.service.dto;

import java.time.OffsetDateTime;
import java.util.List;

public final class EventContracts {

    private EventContracts() {
    }

    public record CreateEventCommand(
        String title,
        String description,
        String imageUrl,
        java.time.LocalDateTime startsAt,
        String venueName,
        String streetAddress,
        String city,
        String stateCode,
        String ageClassification,
        String organizer,
        List<TicketCategoryInput> categories,
        List<SeatSectorInput> sectors
    ) {
        public CreateEventCommand {
            categories = List.copyOf(categories);
            sectors = sectors == null ? List.of() : List.copyOf(sectors);
        }
    }

    public record TicketCategoryInput(
        String code,
        String name,
        long priceInCents,
        AdmissionModeCode admissionMode,
        Integer quantity
    ) {
    }

    public record SeatSectorInput(String name, int positionIndex, List<SeatRowInput> rows) {
        public SeatSectorInput {
            rows = List.copyOf(rows);
        }
    }

    public record SeatRowInput(String label, int positionIndex, List<SeatInput> seats) {
        public SeatRowInput {
            seats = List.copyOf(seats);
        }
    }

    public record SeatInput(String label, int positionIndex, String categoryCode) {
    }

    public record EventSummary(
        Long id,
        String title,
        String imageUrl,
        OffsetDateTime startsAt,
        String city,
        String stateCode,
        long lowestPriceInCents,
        EventStatusCode status
    ) {
    }

    public record EventView(
        Long id,
        String title,
        String description,
        String imageUrl,
        OffsetDateTime startsAt,
        String timeZone,
        Venue venue,
        String ageClassification,
        String organizer,
        EventStatusCode status,
        List<Category> categories,
        List<Sector> sectors
    ) {
        public EventView {
            categories = List.copyOf(categories);
            sectors = List.copyOf(sectors);
        }

        public record Venue(String name, String streetAddress, String city, String stateCode) {
        }

        public record Category(
            Long id,
            String code,
            String name,
            long priceInCents,
            AdmissionModeCode admissionMode,
            int configuredQuantity,
            int availableQuantity,
            int soldQuantity
        ) {
        }

        public record Sector(Long id, String name, int positionIndex, List<Row> rows) {
            public Sector {
                rows = List.copyOf(rows);
            }
        }

        public record Row(Long id, String label, int positionIndex, List<Seat> seats) {
            public Row {
                seats = List.copyOf(seats);
            }
        }

        public record Seat(
            Long id,
            String label,
            int positionIndex,
            String categoryCode,
            String categoryName,
            long priceInCents,
            SeatStatusCode status
        ) {
        }
    }

    public enum AdmissionModeCode {
        GENERAL_ADMISSION,
        ASSIGNED_SEAT
    }

    public enum EventStatusCode {
        DRAFT,
        PUBLISHED,
        SUSPENDED,
        CANCELLED
    }

    public enum SeatStatusCode {
        AVAILABLE,
        RESERVED,
        SOLD,
        UNAVAILABLE
    }
}