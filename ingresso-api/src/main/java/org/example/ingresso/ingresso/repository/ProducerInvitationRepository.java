package org.example.ingresso.ingresso.repository;

import org.example.ingresso.ingresso.model.ProducerInvitation;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface ProducerInvitationRepository extends JpaRepository<ProducerInvitation, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select invitation from ProducerInvitation invitation where invitation.tokenHash = :tokenHash")
    Optional<ProducerInvitation> findByTokenHashForUpdate(@Param("tokenHash") String tokenHash);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select invitation from ProducerInvitation invitation where invitation.id = :id")
    Optional<ProducerInvitation> findByIdForUpdate(@Param("id") Long id);

    boolean existsByEmailIgnoreCaseAndStatusAndExpiresAtAfter(
        String email,
        org.example.ingresso.ingresso.model.enums.ProducerInvitationStatus status,
        Instant now
    );
}