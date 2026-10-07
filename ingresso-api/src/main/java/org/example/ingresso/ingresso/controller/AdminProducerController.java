package org.example.ingresso.ingresso.controller;

import org.example.ingresso.ingresso.dto.CreateProducerInvitationRequest;
import org.example.ingresso.ingresso.dto.ProducerInvitationResponse;
import org.example.ingresso.ingresso.security.UserSS;
import org.example.ingresso.ingresso.service.ProducerInvitationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.example.ingresso.ingresso.config.Constants.SECURITY_ROLE_ADMIN;

@RestController
@RequestMapping("/api/admin")
public class AdminProducerController {

    private final ProducerInvitationService invitationService;

    public AdminProducerController(ProducerInvitationService invitationService) {
        this.invitationService = invitationService;
    }

    @PreAuthorize(SECURITY_ROLE_ADMIN)
    @PostMapping("/convites/produtores")
    public ResponseEntity<ProducerInvitationResponse> inviteProducer(
        @RequestBody @Valid CreateProducerInvitationRequest request,
        @AuthenticationPrincipal UserSS administrator
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(invitationService.inviteProducer(request.email(), administrator));
    }

    @PreAuthorize(SECURITY_ROLE_ADMIN)
    @DeleteMapping("/convites/produtores/{invitationId}")
    public ResponseEntity<Void> revokeInvitation(
        @PathVariable Long invitationId,
        @AuthenticationPrincipal UserSS administrator
    ) {
        invitationService.revokeInvitation(invitationId, administrator);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize(SECURITY_ROLE_ADMIN)
    @DeleteMapping("/produtores/{userId}")
    public ResponseEntity<Void> revokeProducer(
        @PathVariable Long userId,
        @AuthenticationPrincipal UserSS administrator
    ) {
        invitationService.revokeProducer(userId, administrator);
        return ResponseEntity.noContent().build();
    }
}