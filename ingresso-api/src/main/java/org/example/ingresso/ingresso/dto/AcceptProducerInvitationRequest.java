package org.example.ingresso.ingresso.dto;

import jakarta.validation.constraints.NotBlank;

public record AcceptProducerInvitationRequest(
    @NotBlank
    String token
) {
}