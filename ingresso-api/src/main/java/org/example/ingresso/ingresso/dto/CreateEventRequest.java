package org.example.ingresso.ingresso.dto;

import org.example.ingresso.ingresso.model.enums.AdmissionMode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;

public record CreateEventRequest(
    @NotBlank @Size(max = 160) String title,
    @NotBlank @Size(max = 5000) String description,
    @NotBlank @Size(max = 2048) String imageUrl,
    @NotNull LocalDateTime startsAt,
    @NotBlank @Size(max = 200) String venueName,
    @NotBlank @Size(max = 300) String streetAddress,
    @NotBlank @Size(max = 120) String city,
    @NotBlank @Pattern(regexp = "[A-Za-z]{2}") String stateCode,
    @NotBlank @Size(max = 80) String ageClassification,
    @NotBlank @Size(max = 200) String organizer,
    @NotEmpty List<@Valid TicketCategoryInput> categories,
    List<@Valid SeatSectorInput> sectors
) {
    public record TicketCategoryInput(
        @NotBlank @Size(max = 40) String code,
        @NotBlank @Size(max = 120) String name,
        @PositiveOrZero long priceInCents,
        @NotNull AdmissionMode admissionMode,
        Integer quantity
    ) {
    }

    public record SeatSectorInput(
        @NotBlank @Size(max = 100) String name,
        @PositiveOrZero int positionIndex,
        @NotEmpty List<@Valid SeatRowInput> rows
    ) {
    }

    public record SeatRowInput(
        @NotBlank @Size(max = 20) String label,
        @PositiveOrZero int positionIndex,
        @NotEmpty List<@Valid SeatInput> seats
    ) {
    }

    public record SeatInput(
        @NotBlank @Size(max = 20) String label,
        @PositiveOrZero int positionIndex,
        @NotBlank @Size(max = 40) String categoryCode
    ) {
    }
}