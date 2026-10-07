package org.example.ingresso.ingresso.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public record OfflineScanRequest(
    @NotBlank @Pattern(regexp = "[0-9a-fA-F-]{36}") String scanId,
    @NotBlank @Pattern(regexp = "[0-9a-f]{64}") @Size(max = 64) String qrTokenHash,
    @NotNull Instant scannedAt
) {
}