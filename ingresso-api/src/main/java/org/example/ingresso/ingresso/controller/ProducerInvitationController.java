package org.example.ingresso.ingresso.controller;

import org.example.ingresso.ingresso.dto.AcceptProducerInvitationRequest;
import org.example.ingresso.ingresso.security.UserSS;
import org.example.ingresso.ingresso.service.ProducerInvitationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/convites/produtores")
public class ProducerInvitationController {

    private final ProducerInvitationService invitationService;

    public ProducerInvitationController(ProducerInvitationService invitationService) {
        this.invitationService = invitationService;
    }

    @PostMapping("/aceitar")
    public ResponseEntity<Void> accept(
        @RequestBody @Valid AcceptProducerInvitationRequest request,
        @AuthenticationPrincipal UserSS identity
    ) {
        invitationService.acceptProducerInvitation(request.token(), identity);
        return ResponseEntity.noContent().build();
    }
}