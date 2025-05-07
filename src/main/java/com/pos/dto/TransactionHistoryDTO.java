package com.pos.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class TransactionHistoryDTO {
    private Long id;
    private LocalDateTime transactionDate;
    private String receiptNumber;
    private String customerName;
    private BigDecimal totalAmount;
    private String cashierName;

    // Constructors
    public TransactionHistoryDTO() {}
    public TransactionHistoryDTO(Long id, LocalDateTime transactionDate, String receiptNumber, String customerName, BigDecimal totalAmount, String cashierName) {
        this.id = id;
        this.transactionDate = transactionDate;
        this.receiptNumber = receiptNumber;
        this.customerName = customerName;
        this.totalAmount = totalAmount;
        this.cashierName = cashierName;
    }

    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public LocalDateTime getTransactionDate() { return transactionDate; }
    public void setTransactionDate(LocalDateTime transactionDate) { this.transactionDate = transactionDate; }
    public String getReceiptNumber() { return receiptNumber; }
    public void setReceiptNumber(String receiptNumber) { this.receiptNumber = receiptNumber; }
    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
    public String getCashierName() { return cashierName; }
    public void setCashierName(String cashierName) { this.cashierName = cashierName; }
} 