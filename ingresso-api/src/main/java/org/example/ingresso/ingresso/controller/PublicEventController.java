package org.example.ingresso.ingresso.controller;

import org.example.ingresso.ingresso.dto.EventResponse;
import org.example.ingresso.ingresso.dto.EventSummaryResponse;
import org.example.ingresso.ingresso.service.EventService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/eventos")
public class PublicEventController {

    private final EventService eventService;

    public PublicEventController(EventService eventService) {
        this.eventService = eventService;
    }

    @GetMapping
    public ResponseEntity<Page<EventSummaryResponse>> list(
        @PageableDefault(size = 20, sort = "startsAt", direction = Sort.Direction.ASC) Pageable pageable
    ) {
        return ResponseEntity.ok(eventService.listPublic(pageable));
    }

    @GetMapping("/{eventId}")
    public ResponseEntity<EventResponse> get(@PathVariable Long eventId) {
        return ResponseEntity.ok(eventService.getPublic(eventId));
    }
}