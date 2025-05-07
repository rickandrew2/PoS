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

    public Product updateProduct(Long id, Product product, String username) {
        Product existingProduct = productRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Product not found"));
        StringBuilder changes = new StringBuilder();
        if (!existingProduct.getName().equals(product.getName())) {
            changes.append("Name: ").append(existingProduct.getName()).append(" -> ").append(product.getName()).append("; ");
        }
        if (!existingProduct.getDescription().equals(product.getDescription())) {
            changes.append("Description changed; ");
        }
        if (!existingProduct.getPrice().equals(product.getPrice())) {
            changes.append("Price: ").append(existingProduct.getPrice()).append(" -> ").append(product.getPrice()).append("; ");
        }
        if (existingProduct.getVatable() != product.getVatable()) {
            changes.append("VAT status: ").append(existingProduct.getVatable() ? "VATable" : "Non-VATable")
                  .append(" -> ").append(product.getVatable() ? "VATable" : "Non-VATable").append("; ");
        }
        int oldStock = existingProduct.getStockQuantity();
        int newStock = product.getStockQuantity();
        if (oldStock != newStock) {
            changes.append("Stock: ").append(oldStock).append(" -> ").append(newStock).append("; ");
        }
        existingProduct.setName(product.getName());
        existingProduct.setDescription(product.getDescription());
        existingProduct.setPrice(product.getPrice());
        existingProduct.setStockQuantity(product.getStockQuantity());
        existingProduct.setCategory(product.getCategory());
        existingProduct.setActive(product.getActive());
        Product updated = productRepository.save(existingProduct);
        auditLogService.logProductUpdate(username, updated.getName(), updated.getId(), changes.toString());
        return updated;
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
} 