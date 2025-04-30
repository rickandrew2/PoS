package com.pos.controller;

import com.pos.entity.Transaction;
import com.pos.service.TransactionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/transactions")
@CrossOrigin(origins = "*")
public class TransactionController {
    @Autowired
    private TransactionService transactionService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'CASHIER')")
    public ResponseEntity<Transaction> createTransaction(@RequestBody Transaction transaction) {
        return ResponseEntity.ok(transactionService.createTransaction(transaction));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'CASHIER')")
    public ResponseEntity<Transaction> getTransactionById(@PathVariable Long id) {
        return transactionService.getTransactionById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/cashier/{cashierId}")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'CASHIER')")
    public ResponseEntity<List<Transaction>> getTransactionsByCashier(@PathVariable Long cashierId) {
        return ResponseEntity.ok(transactionService.getTransactionsByCashier(cashierId));
    }

    @GetMapping("/date-range")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'CASHIER')")
    public ResponseEntity<List<Transaction>> getTransactionsByDateRange(
            @RequestParam LocalDateTime start,
            @RequestParam LocalDateTime end) {
        return ResponseEntity.ok(transactionService.getTransactionsByDateRange(start, end));
    }
} 