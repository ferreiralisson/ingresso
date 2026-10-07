package org.example.ingresso.ingresso.repository;

import org.example.ingresso.ingresso.model.TicketRefund;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TicketRefundRepository extends JpaRepository<TicketRefund, Long> {
    Optional<TicketRefund> findByTicket_Id(Long ticketId);

    List<TicketRefund> findByEvent_Id(Long eventId);
}