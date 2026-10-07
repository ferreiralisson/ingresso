package org.example.ingresso.ingresso.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record OfflineSyncRequest(
    @NotBlank @Size(max = 36) String manifestId,
    @NotBlank @Size(max = 64) String deviceId,
    @NotEmpty @Size(max = 500) List<@Valid OfflineScanRequest> scans
) {
}