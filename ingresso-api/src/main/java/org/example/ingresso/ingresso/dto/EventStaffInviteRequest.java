package org.example.ingresso.ingresso.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record EventStaffInviteRequest(@NotBlank @Email @Size(max = 320) String email) {
}