package org.example.ingresso.ingresso.controller;

import org.example.ingresso.ingresso.dto.EventResponse;
import org.example.ingresso.ingresso.dto.EventSummaryResponse;
import org.example.ingresso.ingresso.security.UserSS;
import org.example.ingresso.ingresso.service.EventService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.example.ingresso.ingresso.config.Constants.SECURITY_ROLE_ADMIN;

@RestController
@RequestMapping("/api/admin/eventos")
@PreAuthorize(SECURITY_ROLE_ADMIN)
public class AdminEventController {

    private final EventService eventService;

    public AdminEventController(EventService eventService) {
        this.eventService = eventService;
    }

    @GetMapping
    public ResponseEntity<Page<EventSummaryResponse>> list(
        @PageableDefault(size = 20, sort = "startsAt", direction = Sort.Direction.ASC) Pageable pageable
    ) {
        return ResponseEntity.ok(eventService.listAllForAdmin(pageable));
    }

    @GetMapping("/{eventId}")
    public ResponseEntity<EventResponse> get(@PathVariable Long eventId) {
        return ResponseEntity.ok(eventService.getAdminEvent(eventId));
    }

    @PatchMapping("/{eventId}/suspender")
    public ResponseEntity<Void> suspend(@PathVariable Long eventId) {
        eventService.suspend(eventId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{eventId}/cancelar")
    public ResponseEntity<Void> cancel(@PathVariable Long eventId, @AuthenticationPrincipal UserSS administrator) {
        eventService.cancel(eventId, administrator);
        return ResponseEntity.noContent().build();
    }
}