package org.example.ingresso.ingresso.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record EntryValidationRequest(
    @NotBlank @Pattern(regexp = "[0-9a-fA-F-]{36}") String scanId,
    @NotBlank @Size(max = 128) String qrCodeValue
) {
}