package org.example.ingresso.ingresso.controller;

import org.example.ingresso.ingresso.dto.CreateEventRequest;
import org.example.ingresso.ingresso.dto.EventResponse;
import org.example.ingresso.ingresso.dto.EventSummaryResponse;
import org.example.ingresso.ingresso.service.dto.EventContracts.CreateEventCommand;
import org.example.ingresso.ingresso.service.dto.EventContracts.AdmissionModeCode;
import org.example.ingresso.ingresso.service.dto.EventContracts.EventSummary;
import org.example.ingresso.ingresso.service.dto.EventContracts.EventView;
import org.example.ingresso.ingresso.service.dto.EventContracts.SeatInput;
import org.example.ingresso.ingresso.service.dto.EventContracts.SeatRowInput;
import org.example.ingresso.ingresso.service.dto.EventContracts.SeatSectorInput;
import org.example.ingresso.ingresso.service.dto.EventContracts.TicketCategoryInput;
import org.springframework.data.domain.Page;

import java.util.List;

final class EventHttpMapper {

    private EventHttpMapper() {
    }

    static Page<EventSummaryResponse> toSummaryPage(
        org.example.ingresso.ingresso.service.dto.PageResult<EventSummary> result
    ) {
        List<EventSummaryResponse> content = result.content().stream()
            .map(EventHttpMapper::toSummaryResponse)
            .toList();
        return PageHttpMapper.toPage(new org.example.ingresso.ingresso.service.dto.PageResult<>(
            content, result.pageNumber(), result.pageSize(), result.totalElements(), result.sort()
        ));
    }

    static EventSummaryResponse toSummaryResponse(EventSummary summary) {
        return new EventSummaryResponse(
            summary.id(), summary.title(), summary.imageUrl(), summary.startsAt(), summary.city(),
            summary.stateCode(), summary.lowestPriceInCents(),
            org.example.ingresso.ingresso.model.enums.EventStatus.valueOf(summary.status().name())
        );
    }

    static EventResponse toResponse(EventView view) {
        List<EventResponse.Category> categories = view.categories().stream()
            .map(category -> new EventResponse.Category(
                category.id(), category.code(), category.name(), category.priceInCents(),
                org.example.ingresso.ingresso.model.enums.AdmissionMode.valueOf(category.admissionMode().name()),
                category.configuredQuantity(), category.availableQuantity(), category.soldQuantity()
            ))
            .toList();
        List<EventResponse.Sector> sectors = view.sectors().stream()
            .map(sector -> new EventResponse.Sector(
                sector.id(), sector.name(), sector.positionIndex(), sector.rows().stream()
                    .map(row -> new EventResponse.Row(
                        row.id(), row.label(), row.positionIndex(), row.seats().stream()
                            .map(seat -> new EventResponse.Seat(
                                seat.id(), seat.label(), seat.positionIndex(), seat.categoryCode(), seat.categoryName(),
                                seat.priceInCents(),
                                org.example.ingresso.ingresso.model.enums.SeatStatus.valueOf(seat.status().name())
                            ))
                            .toList()
                    ))
                    .toList()
            ))
            .toList();
        EventView.Venue venue = view.venue();
        return new EventResponse(
            view.id(), view.title(), view.description(), view.imageUrl(), view.startsAt(), view.timeZone(),
            new EventResponse.Venue(venue.name(), venue.streetAddress(), venue.city(), venue.stateCode()),
            view.ageClassification(), view.organizer(),
            org.example.ingresso.ingresso.model.enums.EventStatus.valueOf(view.status().name()), categories, sectors
        );
    }

    static CreateEventCommand toCommand(CreateEventRequest request) {
        List<TicketCategoryInput> categories = request.categories().stream()
            .map(category -> new TicketCategoryInput(
                category.code(), category.name(), category.priceInCents(),
                AdmissionModeCode.valueOf(category.admissionMode().name()), category.quantity()
            ))
            .toList();
        List<SeatSectorInput> sectors = request.sectors() == null ? List.of() : request.sectors().stream()
            .map(sector -> new SeatSectorInput(
                sector.name(), sector.positionIndex(), sector.rows().stream()
                    .map(row -> new SeatRowInput(
                        row.label(), row.positionIndex(), row.seats().stream()
                            .map(seat -> new SeatInput(seat.label(), seat.positionIndex(), seat.categoryCode()))
                            .toList()
                    ))
                    .toList()
            ))
            .toList();
        return new CreateEventCommand(
            request.title(), request.description(), request.imageUrl(), request.startsAt(), request.venueName(),
            request.streetAddress(), request.city(), request.stateCode(), request.ageClassification(),
            request.organizer(), categories, sectors
        );
    }
}