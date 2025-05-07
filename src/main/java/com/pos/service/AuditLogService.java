package com.pos.service;

import com.pos.entity.AuditLog;
import com.pos.repository.AuditLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;

@Service
public class AuditLogService {
    @Autowired
    private AuditLogRepository auditLogRepository;

    public void logAction(String username, String action, String details) {
        AuditLog log = new AuditLog();
        log.setUsername(username);
        log.setAction(action);
        log.setDetails(details);
        auditLogRepository.save(log);
    }

    // Inventory Management Logging Methods
    public void logProductCreation(String username, String productName, Long productId) {
        logAction(username, "PRODUCT_CREATE", 
            String.format("Created new product: %s (ID: %d)", productName, productId));
    }

    public void logProductUpdate(String username, String productName, Long productId, String changes) {
        logAction(username, "PRODUCT_UPDATE", 
            String.format("Updated product: %s (ID: %d). Changes: %s", productName, productId, changes));
    }

    public void logProductDeletion(String username, String productName, Long productId) {
        logAction(username, "PRODUCT_DELETE", 
            String.format("Deleted product: %s (ID: %d)", productName, productId));
    }

    public void logStockAdjustment(String username, String productName, Long productId, 
                                 int oldQuantity, int newQuantity, String reason) {
        logAction(username, "STOCK_ADJUSTMENT", 
            String.format("Adjusted stock for product: %s (ID: %d). Old quantity: %d, New quantity: %d. Reason: %s", 
                productName, productId, oldQuantity, newQuantity, reason));
    }

    public void logCategoryCreation(String username, String categoryName, Long categoryId) {
        logAction(username, "CATEGORY_CREATE", 
            String.format("Created new category: %s (ID: %d)", categoryName, categoryId));
    }

    public void logCategoryUpdate(String username, String categoryName, Long categoryId, String changes) {
        logAction(username, "CATEGORY_UPDATE", 
            String.format("Updated category: %s (ID: %d). Changes: %s", categoryName, categoryId, changes));
    }

    public void logCategoryDeletion(String username, String categoryName, Long categoryId) {
        logAction(username, "CATEGORY_DELETE", 
            String.format("Deleted category: %s (ID: %d)", categoryName, categoryId));
    }

    public void logBulkImport(String username, int itemsImported, String fileName) {
        logAction(username, "BULK_IMPORT", 
            String.format("Imported %d items from file: %s", itemsImported, fileName));
    }

    public void logBulkExport(String username, int itemsExported, String fileName) {
        logAction(username, "BULK_EXPORT", 
            String.format("Exported %d items to file: %s", itemsExported, fileName));
    }

    public void logPriceChange(String username, String productName, Long productId, 
                             String oldPrice, String newPrice) {
        logAction(username, "PRICE_CHANGE", 
            String.format("Changed price for product: %s (ID: %d). Old price: %s, New price: %s", 
                productName, productId, oldPrice, newPrice));
    }

    public void logProductStatusChange(String username, String productName, Long productId, 
                                     boolean oldStatus, boolean newStatus) {
        logAction(username, "PRODUCT_STATUS_CHANGE", 
            String.format("Changed status for product: %s (ID: %d). Old status: %s, New status: %s", 
                productName, productId, oldStatus ? "Active" : "Inactive", newStatus ? "Active" : "Inactive"));
    }
} 