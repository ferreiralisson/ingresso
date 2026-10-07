package org.example.ingresso.ingresso.repository;

import org.example.ingresso.ingresso.model.EventStaffInvitation;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface EventStaffInvitationRepository extends JpaRepository<EventStaffInvitation, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select invitation from EventStaffInvitation invitation where invitation.tokenHash = :tokenHash")
    Optional<EventStaffInvitation> findWithLockByTokenHash(@Param("tokenHash") String tokenHash);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select invitation from EventStaffInvitation invitation where invitation.id = :id")
    Optional<EventStaffInvitation> findWithLockById(@Param("id") Long id);

    boolean existsByEvent_IdAndEmailIgnoreCaseAndAcceptedAtIsNullAndRevokedAtIsNullAndExpiresAtAfter(
        Long eventId, String email, Instant now
    );

    boolean existsByEvent_IdAndAcceptedBy_IdAndRevokedAtIsNull(Long eventId, Long userId);

    List<EventStaffInvitation> findByEvent_IdAndAcceptedAtIsNotNullAndRevokedAtIsNullOrderByAcceptedAtAsc(Long eventId);
}