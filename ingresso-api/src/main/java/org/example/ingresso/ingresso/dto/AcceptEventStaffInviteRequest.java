package org.example.ingresso.ingresso.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AcceptEventStaffInviteRequest(@NotBlank @Size(max = 128) String token) {
}