package org.example.ingresso.ingresso.dto;

import java.time.Instant;
import java.util.List;

public record OfflineSyncResponse(String manifestId, Instant synchronizedAt, List<EntryValidationResponse> results) {
}