package org.example.ingresso.ingresso.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record EntryCorrectionRequest(@NotBlank @Size(max = 500) String reason) {
}