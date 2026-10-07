package org.example.ingresso.ingresso.repository;

import org.example.ingresso.ingresso.model.EntryCheckInCorrection;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EntryCheckInCorrectionRepository extends JpaRepository<EntryCheckInCorrection, Long> {
    boolean existsByTicket_Id(Long ticketId);

    java.util.Optional<EntryCheckInCorrection> findByTicket_Id(Long ticketId);
}