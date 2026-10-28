package org.example.ingresso.ingresso.service.dto;

import java.time.Instant;
import java.util.Set;
import java.util.List;

public final class CoreServiceContracts {

    private CoreServiceContracts() {
    }

    public record AcceptInvitationCommand(String token, Long userId, String email) {
    }

    public record CreateUserCommand(String name, String email, String password) {
    }

    public record UserView(Long id, String name, String email, Set<String> roles) {
        public UserView {
            roles = Set.copyOf(roles);
        }
    }

    public record ProducerInvitationResult(Long id, String email, String token, Instant expiresAt) {
    }

    public record EventStaffInvite(Long id, Long eventId, String email, String token, Instant expiresAt) {
    }

    public record EventStaffGrant(Long id, Long userId, String email, Instant acceptedAt) {
    }

    public record FinancialEventReport(
        Long eventId,
        String eventTitle,
        long paidOrderCount,
        long paidTicketCount,
        long refundedTicketCount,
        long grossSalesInCents,
        long simulatedRefundsInCents,
        long remainingGrossInCents
    ) {
    }

    public record FinancialReport(
        long paidOrderCount,
        long paidTicketCount,
        long refundedTicketCount,
        long grossSalesInCents,
        long simulatedRefundsInCents,
        long remainingGrossInCents,
        List<FinancialEventReport> events
    ) {
        public FinancialReport {
            events = List.copyOf(events);
        }
    }
}