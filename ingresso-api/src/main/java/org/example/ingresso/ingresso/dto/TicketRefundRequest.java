package org.example.ingresso.ingresso.dto;

import jakarta.validation.constraints.Size;

public record TicketRefundRequest(@Size(max = 500) String reason) {
}