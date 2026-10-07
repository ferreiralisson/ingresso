package org.example.ingresso.ingresso.repository;

import org.example.ingresso.ingresso.model.EntryCheckIn;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EntryCheckInRepository extends JpaRepository<EntryCheckIn, Long> {
    Optional<EntryCheckIn> findByOperator_IdAndClientScanId(Long operatorId, String clientScanId);

    Page<EntryCheckIn> findByEvent_IdOrderByProcessedAtDesc(Long eventId, Pageable pageable);
}