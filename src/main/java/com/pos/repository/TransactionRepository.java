package com.pos.repository;

import com.pos.entity.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    List<Transaction> findByCashierId(Long cashierId);
    List<Transaction> findByTransactionDateBetween(LocalDateTime start, LocalDateTime end);
    Optional<Transaction> findByReceiptNumber(String receiptNumber);
    List<Transaction> findByCustomerId(Long customerId);
    Page<Transaction> findByTransactionDateBetween(LocalDateTime start, LocalDateTime end, Pageable pageable);
} 