package com.pos.service;

import com.pos.entity.Product;
import com.pos.repository.ProductRepository;
import com.pos.service.AuditLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
public class ProductService {
    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private AuditLogService auditLogService;

    public Product createProduct(Product product, String username) {
        if (productRepository.existsByName(product.getName())) {
            throw new RuntimeException("Product name already exists");
        }
        Product created = productRepository.save(product);
        auditLogService.logProductCreation(username, created.getName(), created.getId());
        return created;
    }

    public void deleteProduct(Long id, String username) {
        Product product = productRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Product not found"));
        product.setActive(false);
        productRepository.save(product);
        auditLogService.logProductDeletion(username, product.getName(), product.getId());
    }

    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    public List<Product> getActiveProducts() {
        return productRepository.findByActiveTrue();
    }

    public List<Product> getProductsByCategory(Long categoryId) {
        return productRepository.findByCategoryId(categoryId);
    }

    public Optional<Product> getProductById(Long id) {
        return productRepository.findById(id);
    }

    public void updateStock(Long id, Integer quantity) {
        Product product = productRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Product not found"));
        
        int newStock = product.getStockQuantity() + quantity;
        if (newStock < 0) {
            throw new RuntimeException("Insufficient stock");
        }
        
        product.setStockQuantity(newStock);
        productRepository.save(product);
    }

    public long getTotalProducts() {
        return productRepository.countByActiveTrue();
    }

    public Product transferOnHoldStock(Long id, String username) {
        Product product = productRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Product not found"));
        int maxStock = 50;
        int currentStock = product.getStockQuantity();
        int onHold = product.getOnHoldStock();
        if (currentStock >= maxStock || onHold <= 0) {
            return product; // Nothing to transfer
        }
        int spaceLeft = maxStock - currentStock;
        int toTransfer = Math.min(spaceLeft, onHold);
        product.setStockQuantity(currentStock + toTransfer);
        product.setOnHoldStock(onHold - toTransfer);
        Product updated = productRepository.save(product);
        auditLogService.logProductUpdate(username, updated.getName(), updated.getId(), "Transferred " + toTransfer + " from on-hold stock to main stock.");
        return updated;
    }

    public Product updateProduct(Product product, String username) {
        Product updated = productRepository.save(product);
        auditLogService.logProductUpdate(username, updated.getName(), updated.getId(), "Stock updated.");
        return updated;
    }
} 