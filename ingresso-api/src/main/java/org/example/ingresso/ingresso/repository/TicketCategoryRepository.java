package org.example.ingresso.ingresso.repository;

import org.example.ingresso.ingresso.model.TicketCategory;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.Collection;
import java.util.List;

public interface TicketCategoryRepository extends JpaRepository<TicketCategory, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = "event")
    List<TicketCategory> findWithLockByEvent_IdAndIdIn(Long eventId, Collection<Long> ids);
}