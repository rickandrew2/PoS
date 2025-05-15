package com.pos.controller;

import com.pos.entity.Transaction;
import com.pos.service.TransactionService;
import com.pos.dto.TransactionHistoryDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;

@RestController
@RequestMapping("/sales/api")
public class SalesApiController {
    @Autowired
    private TransactionService transactionService;

    @GetMapping("/history")
    @PreAuthorize("hasRole('ADMINISTRATOR') or hasRole('CASHIER')")
    public ResponseEntity<?> getTransactionHistory(
            @RequestParam(value = "startDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(value = "endDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size
    ) {
        LocalDateTime start = (startDate != null) ? startDate.atStartOfDay() : LocalDate.of(1970,1,1).atStartOfDay();
        LocalDateTime end = (endDate != null) ? endDate.atTime(LocalTime.MAX) : LocalDate.now().atTime(LocalTime.MAX);
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "transactionDate"));
        Page<Transaction> txPage = transactionService.getTransactionsByDateRangePaged(start, end, pageable);
        Page<TransactionHistoryDTO> dtoPage = txPage.map(tx -> {
            String customerName = "Walk-in";
            if (tx.getCustomer() != null && tx.getCustomer().getName() != null && !tx.getCustomer().getName().isEmpty()) {
                customerName = tx.getCustomer().getName();
            } else if (tx.getCustomerType() != null && tx.getCustomerType().getTitle() != null && !tx.getCustomerType().getTitle().isEmpty()) {
                customerName = tx.getCustomerType().getTitle();
            }
            String cashierName = "N/A";
            if (tx.getCashier() != null) {
                if (tx.getCashier().getUsername() != null && !tx.getCashier().getUsername().isEmpty()) {
                    cashierName = tx.getCashier().getUsername();
                } else if (tx.getCashier().getFullName() != null && !tx.getCashier().getFullName().isEmpty()) {
                    cashierName = tx.getCashier().getFullName();
                }
            }
            return new TransactionHistoryDTO(
                tx.getId(),
                tx.getTransactionDate(),
                tx.getReceiptNumber(),
                customerName,
                tx.getTotalAmount(),
                cashierName
            );
        });
        HashMap<String, Object> response = new HashMap<>();
        response.put("content", dtoPage.getContent());
        response.put("totalPages", dtoPage.getTotalPages());
        response.put("totalElements", dtoPage.getTotalElements());
        response.put("page", dtoPage.getNumber());
        response.put("size", dtoPage.getSize());
        return ResponseEntity.ok(response);
    }
} 