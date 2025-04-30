package com.pos.repository;

import com.pos.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    List<Transaction> findByCashierId(Long cashierId);
    List<Transaction> findByTransactionDateBetween(LocalDateTime start, LocalDateTime end);
    boolean existsByReceiptNumber(String receiptNumber);
} 