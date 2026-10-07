package org.example.ingresso.ingresso.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.List;

public record CreateOrderRequest(
    @NotNull Long eventId,
    @NotEmpty List<@Valid Item> items
) {
    public record Item(
        @NotNull Long categoryId,
        @Positive int quantity,
        List<@Positive Long> seatIds
    ) {
    }
}