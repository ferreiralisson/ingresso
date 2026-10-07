package org.example.ingresso.ingresso.repository;

import org.example.ingresso.ingresso.model.IssuedTicket;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface IssuedTicketRepository extends JpaRepository<IssuedTicket, Long> {
    List<IssuedTicket> findByOrder_IdOrderByIdAsc(Long orderId);

    long countByOrder_Id(Long orderId);

    java.util.List<IssuedTicket> findByOrder_Event_IdOrderByIdAsc(Long eventId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select ticket from IssuedTicket ticket where ticket.qrToken = :qrToken")
    java.util.Optional<IssuedTicket> findWithLockByQrToken(@Param("qrToken") String qrToken);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select ticket from IssuedTicket ticket where ticket.qrTokenHash = :qrTokenHash")
    java.util.Optional<IssuedTicket> findWithLockByQrTokenHash(@Param("qrTokenHash") String qrTokenHash);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select ticket from IssuedTicket ticket where ticket.id = :ticketId")
    java.util.Optional<IssuedTicket> findWithLockById(@Param("ticketId") Long ticketId);
}