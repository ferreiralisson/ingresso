package org.example.ingresso.ingresso.repository;

import org.example.ingresso.ingresso.model.EventSeat;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.Collection;
import java.util.List;

public interface EventSeatRepository extends JpaRepository<EventSeat, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {"category", "row", "row.sector", "row.sector.event"})
    List<EventSeat> findWithLockByIdIn(Collection<Long> ids);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {"category", "row", "row.sector", "row.sector.event"})
    List<EventSeat> findWithLockByReservedOrderId(Long orderId);
}