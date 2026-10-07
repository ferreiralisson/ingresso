package org.example.ingresso.ingresso.controller;

import org.example.ingresso.ingresso.dto.CreateEventRequest;
import org.example.ingresso.ingresso.dto.EventResponse;
import org.example.ingresso.ingresso.dto.EventSummaryResponse;
import org.example.ingresso.ingresso.security.UserSS;
import org.example.ingresso.ingresso.service.EventService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.example.ingresso.ingresso.config.Constants.SECURITY_ROLE_PRODUCER;

@RestController
@RequestMapping("/api/produtor/eventos")
@PreAuthorize(SECURITY_ROLE_PRODUCER)
public class ProducerEventController {

    private final EventService eventService;

    public ProducerEventController(EventService eventService) {
        this.eventService = eventService;
    }

    @GetMapping
    public ResponseEntity<Page<EventSummaryResponse>> list(
        @AuthenticationPrincipal UserSS producer,
        @PageableDefault(size = 20, sort = "startsAt", direction = Sort.Direction.ASC) Pageable pageable
    ) {
        return ResponseEntity.ok(eventService.listProducerEvents(producer, pageable));
    }

    @GetMapping("/{eventId}")
    public ResponseEntity<EventResponse> get(
        @PathVariable Long eventId,
        @AuthenticationPrincipal UserSS producer
    ) {
        return ResponseEntity.ok(eventService.getProducerEvent(eventId, producer));
    }

    @PostMapping
    public ResponseEntity<EventResponse> create(
        @RequestBody @Valid CreateEventRequest request,
        @AuthenticationPrincipal UserSS producer
    ) {
        EventResponse response = eventService.create(request, producer);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{eventId}")
    public ResponseEntity<EventResponse> update(
        @PathVariable Long eventId,
        @RequestBody @Valid CreateEventRequest request,
        @AuthenticationPrincipal UserSS producer
    ) {
        return ResponseEntity.ok(eventService.update(eventId, request, producer));
    }

    @PatchMapping("/{eventId}/reativar")
    public ResponseEntity<EventResponse> reactivate(
        @PathVariable Long eventId,
        @AuthenticationPrincipal UserSS producer
    ) {
        return ResponseEntity.ok(eventService.reactivate(eventId, producer));
    }

    @PatchMapping("/{eventId}/cancelar")
    public ResponseEntity<Void> cancel(
        @PathVariable Long eventId,
        @AuthenticationPrincipal UserSS producer
    ) {
        eventService.cancel(eventId, producer);
        return ResponseEntity.noContent().build();
    }
}