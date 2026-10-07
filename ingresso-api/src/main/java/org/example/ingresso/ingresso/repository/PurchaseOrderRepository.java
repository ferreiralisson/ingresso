package org.example.ingresso.ingresso.repository;

import org.example.ingresso.ingresso.model.PurchaseOrder;
import org.example.ingresso.ingresso.model.enums.OrderStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.time.Instant;
import java.util.Optional;

public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, Long> {

    Optional<PurchaseOrder> findByBuyer_IdAndIdempotencyKey(Long buyerId, String idempotencyKey);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<PurchaseOrder> findWithLockByIdAndBuyer_Id(Long id, Long buyerId);

    Page<PurchaseOrder> findByBuyer_Id(Long buyerId, Pageable pageable);

    java.util.List<PurchaseOrder> findTop100ByStatusAndReservationExpiresAtLessThanEqualOrderByIdAsc(
        OrderStatus status,
        Instant now
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<PurchaseOrder> findWithLockById(Long id);

    java.util.List<PurchaseOrder> findByEvent_IdAndStatus(Long eventId, OrderStatus status);
}