package org.example.ingresso.ingresso.repository;

import org.example.ingresso.ingresso.model.PaymentAttempt;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaymentAttemptRepository extends JpaRepository<PaymentAttempt, Long> {

    Optional<PaymentAttempt> findFirstByOrder_IdOrderByStartedAtDesc(Long orderId);
}