package org.example.ingresso.ingresso.repository;

import org.example.ingresso.ingresso.model.Event;
import org.example.ingresso.ingresso.model.enums.EventStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;
import jakarta.persistence.LockModeType;

import java.util.Optional;

public interface EventRepository extends JpaRepository<Event, Long> {

    Page<Event> findByStatus(EventStatus status, Pageable pageable);

    Page<Event> findByProducer_Id(Long producerId, Pageable pageable);

    Optional<Event> findByIdAndProducer_Id(Long id, Long producerId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Event> findWithLockById(Long id);
}