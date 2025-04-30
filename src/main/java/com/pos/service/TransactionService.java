package com.pos.service;

import com.pos.entity.*;
import com.pos.repository.TransactionRepository;
import com.pos.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class TransactionService {
    private static final BigDecimal VAT_RATE = new BigDecimal("0.12");
    private static final BigDecimal PWD_SENIOR_DISCOUNT_RATE = new BigDecimal("0.20");

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private ProductRepository productRepository;

    @Transactional
    public Transaction createTransaction(Transaction transaction) {
        // Generate receipt number
        String receiptNumber = generateReceiptNumber();
        transaction.setReceiptNumber(receiptNumber);
        transaction.setTransactionDate(LocalDateTime.now());

        // Calculate totals
        calculateTotals(transaction);

        // Update stock quantities
        updateStockQuantities(transaction);

        return transactionRepository.save(transaction);
    }

    private String generateReceiptNumber() {
        // Generate a 6-digit receipt number
        long count = transactionRepository.count() + 1;
        return String.format("%06d", count);
    }

    private void calculateTotals(Transaction transaction) {
        BigDecimal subtotal = BigDecimal.ZERO;

        // Calculate subtotal
        for (TransactionItem item : transaction.getItems()) {
            BigDecimal itemTotal = item.getUnitPrice().multiply(new BigDecimal(item.getQuantity()));
            item.setSubtotal(itemTotal);
            subtotal = subtotal.add(itemTotal);
        }

        transaction.setSubtotal(subtotal);

        // Calculate VAT
        BigDecimal vatAmount = BigDecimal.ZERO;
        if (transaction.isVatIncluded()) {
            vatAmount = subtotal.multiply(VAT_RATE).setScale(2, RoundingMode.HALF_UP);
        }
        transaction.setVatAmount(vatAmount);

        // Calculate discount
        BigDecimal discountAmount = BigDecimal.ZERO;
        if (transaction.isPwdDiscount() || transaction.isSeniorDiscount()) {
            discountAmount = subtotal.multiply(PWD_SENIOR_DISCOUNT_RATE).setScale(2, RoundingMode.HALF_UP);
        }
        transaction.setDiscountAmount(discountAmount);

        // Calculate final total
        BigDecimal total = subtotal.add(vatAmount).subtract(discountAmount);
        transaction.setTotalAmount(total);
    }

    private void updateStockQuantities(Transaction transaction) {
        for (TransactionItem item : transaction.getItems()) {
            Product product = productRepository.findById(item.getProduct().getId())
                .orElseThrow(() -> new RuntimeException("Product not found"));
            
            int newStock = product.getStockQuantity() - item.getQuantity();
            if (newStock < 0) {
                throw new RuntimeException("Insufficient stock for product: " + product.getName());
            }
            
            product.setStockQuantity(newStock);
            productRepository.save(product);
        }
    }

    public List<Transaction> getTransactionsByCashier(Long cashierId) {
        return transactionRepository.findByCashierId(cashierId);
    }

    public List<Transaction> getTransactionsByDateRange(LocalDateTime start, LocalDateTime end) {
        return transactionRepository.findByTransactionDateBetween(start, end);
    }

    public Optional<Transaction> getTransactionById(Long id) {
        return transactionRepository.findById(id);
    }
} 