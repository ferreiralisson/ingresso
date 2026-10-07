package org.example.ingresso.ingresso.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record CreateProducerInvitationRequest(
    @Email
    @NotBlank
    String email
) {
}