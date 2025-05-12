package com.pos.controller;

import com.pos.entity.Product;
import com.pos.entity.Category;
import com.pos.service.ProductService;
import com.pos.service.CategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Map;
import java.math.BigDecimal;

@RestController
@RequestMapping("/api/products")
@CrossOrigin(origins = "*")
public class ProductController {
    @Autowired
    private ProductService productService;

    @Autowired
    private CategoryService categoryService;

    @GetMapping
    public ResponseEntity<List<Product>> getAllProducts() {
        return ResponseEntity.ok(productService.getAllProducts());
    }

    @GetMapping("/active")
    public ResponseEntity<List<Product>> getActiveProducts() {
        return ResponseEntity.ok(productService.getActiveProducts());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Product> getProductById(@PathVariable Long id) {
        return productService.getProductById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/category/{categoryId}")
    public ResponseEntity<List<Product>> getProductsByCategory(@PathVariable Long categoryId) {
        return ResponseEntity.ok(productService.getProductsByCategory(categoryId));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'INVENTORY_PERSONNEL')")
    public ResponseEntity<Product> createProduct(@RequestBody Map<String, Object> payload) {
        // Validate required fields
        if (payload.get("name") == null || payload.get("name").toString().trim().isEmpty()) {
            throw new IllegalArgumentException("Product name is required");
        }
        if (payload.get("price") == null) {
            throw new IllegalArgumentException("Product price is required");
        }
        if (payload.get("categoryId") == null || payload.get("categoryId").toString().trim().isEmpty()) {
            throw new IllegalArgumentException("Category is required");
        }
        if (payload.get("stockQuantity") == null) {
            throw new IllegalArgumentException("Initial stock quantity is required");
        }

        Product product = new Product();
        product.setName(payload.get("name").toString().trim());
        product.setDescription(payload.get("description") != null ? payload.get("description").toString().trim() : "");
        product.setPrice(new BigDecimal(payload.get("price").toString()));
        product.setStockQuantity(Integer.parseInt(payload.get("stockQuantity").toString()));
        product.setVatable(true);

        try {
            Long categoryId = Long.parseLong(payload.get("categoryId").toString());
            Category category = categoryService.getCategoryById(categoryId)
                .orElseThrow(() -> new IllegalArgumentException("Category not found"));
            product.setCategory(category);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid category ID");
        }

        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseEntity.ok(productService.createProduct(product, username));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'INVENTORY_PERSONNEL')")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        productService.deleteProduct(id, username);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{id}/stock")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'INVENTORY_PERSONNEL')")
    public ResponseEntity<Product> updateStock(@PathVariable Long id, @RequestBody Map<String, Object> payload) {
        if (payload.get("adjustment") == null) {
            throw new IllegalArgumentException("Stock adjustment value is required");
        }

        Product product = productService.getProductById(id)
            .orElseThrow(() -> new IllegalArgumentException("Product not found"));

        try {
            int adjustment = Integer.parseInt(payload.get("adjustment").toString());
            int newStock = product.getStockQuantity() + adjustment;
            
            if (newStock < 0) {
                throw new IllegalArgumentException("Insufficient stock. Current stock: " + product.getStockQuantity());
            }

            product.setStockQuantity(newStock);
            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            return ResponseEntity.ok(productService.updateProduct(product, username));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid stock adjustment value");
        }
    }

    @PutMapping("/{id}/transfer-on-hold")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'INVENTORY_PERSONNEL')")
    public ResponseEntity<Product> transferOnHoldStock(@PathVariable Long id) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        Product updatedProduct = productService.transferOnHoldStock(id, username);
        return ResponseEntity.ok(updatedProduct);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'INVENTORY_PERSONNEL')")
    public ResponseEntity<Product> updateProduct(@PathVariable Long id, @RequestBody Map<String, Object> payload) {
        Product product = productService.getProductById(id)
            .orElseThrow(() -> new IllegalArgumentException("Product not found"));

        if (payload.get("name") != null) product.setName(payload.get("name").toString());
        if (payload.get("description") != null) product.setDescription(payload.get("description").toString());
        if (payload.get("price") != null) product.setPrice(new java.math.BigDecimal(payload.get("price").toString()));
        if (payload.get("categoryId") != null) {
            Long categoryId = Long.parseLong(payload.get("categoryId").toString());
            Category category = categoryService.getCategoryById(categoryId)
                .orElseThrow(() -> new IllegalArgumentException("Category not found"));
            product.setCategory(category);
        }
        if (payload.get("stockQuantity") != null) product.setStockQuantity(Integer.parseInt(payload.get("stockQuantity").toString()));
        if (payload.get("vatable") != null) product.setVatable(Boolean.parseBoolean(payload.get("vatable").toString()));

        String username = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseEntity.ok(productService.updateProduct(product, username));
    }
} 