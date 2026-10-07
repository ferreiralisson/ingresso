package org.example.ingresso.ingresso.controller;

import org.example.ingresso.ingresso.dto.AcceptEventStaffInviteRequest;
import org.example.ingresso.ingresso.dto.EventStaffGrantResponse;
import org.example.ingresso.ingresso.dto.EventStaffInviteRequest;
import org.example.ingresso.ingresso.dto.EventStaffInviteResponse;
import org.example.ingresso.ingresso.security.UserSS;
import org.example.ingresso.ingresso.service.EventStaffService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/entrada")
public class EventStaffController {

    private final EventStaffService eventStaffService;

    public EventStaffController(EventStaffService eventStaffService) {
        this.eventStaffService = eventStaffService;
    }

    @PostMapping("/eventos/{eventId}/equipe")
    public ResponseEntity<EventStaffInviteResponse> invite(
        @PathVariable Long eventId,
        @RequestBody @Valid EventStaffInviteRequest request,
        @AuthenticationPrincipal UserSS actor
    ) {
        return ResponseEntity.status(201).body(eventStaffService.invite(eventId, request.email(), actor));
    }

    @GetMapping("/eventos/{eventId}/equipe")
    public ResponseEntity<List<EventStaffGrantResponse>> list(
        @PathVariable Long eventId,
        @AuthenticationPrincipal UserSS actor
    ) {
        return ResponseEntity.ok(eventStaffService.list(eventId, actor));
    }

    @DeleteMapping("/eventos/{eventId}/equipe/{invitationId}")
    public ResponseEntity<Void> revoke(
        @PathVariable Long eventId,
        @PathVariable Long invitationId,
        @AuthenticationPrincipal UserSS actor
    ) {
        eventStaffService.revoke(eventId, invitationId, actor);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/convites/equipe/aceitar")
    public ResponseEntity<Void> accept(
        @RequestBody @Valid AcceptEventStaffInviteRequest request,
        @AuthenticationPrincipal UserSS identity
    ) {
        eventStaffService.accept(request.token(), identity);
        return ResponseEntity.noContent().build();
    }
}