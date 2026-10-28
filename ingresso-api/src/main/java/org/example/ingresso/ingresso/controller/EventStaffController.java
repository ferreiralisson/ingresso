package org.example.ingresso.ingresso.controller;

import org.example.ingresso.ingresso.dto.AcceptEventStaffInviteRequest;
import org.example.ingresso.ingresso.dto.EventStaffGrantResponse;
import org.example.ingresso.ingresso.dto.EventStaffInviteRequest;
import org.example.ingresso.ingresso.dto.EventStaffInviteResponse;
import org.example.ingresso.ingresso.security.UserSS;
import org.example.ingresso.ingresso.service.EventStaffService;
import org.example.ingresso.ingresso.service.dto.CoreServiceContracts.AcceptInvitationCommand;
import org.example.ingresso.ingresso.service.dto.CoreServiceContracts.EventStaffInvite;
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
        EventStaffInvite invite = eventStaffService.invite(eventId, request.email(), actor.getId());
        return ResponseEntity.status(201).body(new EventStaffInviteResponse(
            invite.id(), invite.eventId(), invite.email(), invite.token(), invite.expiresAt()
        ));
    }

    @GetMapping("/eventos/{eventId}/equipe")
    public ResponseEntity<List<EventStaffGrantResponse>> list(
        @PathVariable Long eventId,
        @AuthenticationPrincipal UserSS actor
    ) {
        return ResponseEntity.ok(eventStaffService.list(eventId, actor.getId()).stream()
            .map(grant -> new EventStaffGrantResponse(
                grant.id(), grant.userId(), grant.email(), grant.acceptedAt()
            ))
            .toList());
    }

    @DeleteMapping("/eventos/{eventId}/equipe/{invitationId}")
    public ResponseEntity<Void> revoke(
        @PathVariable Long eventId,
        @PathVariable Long invitationId,
        @AuthenticationPrincipal UserSS actor
    ) {
        eventStaffService.revoke(eventId, invitationId, actor.getId());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/convites/equipe/aceitar")
    public ResponseEntity<Void> accept(
        @RequestBody @Valid AcceptEventStaffInviteRequest request,
        @AuthenticationPrincipal UserSS identity
    ) {
        eventStaffService.accept(new AcceptInvitationCommand(request.token(), identity.getId(), identity.getUsername()));
        return ResponseEntity.noContent().build();
    }
}