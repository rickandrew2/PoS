package com.pos.controller;

import com.pos.entity.Category;
import com.pos.entity.Product;
import com.pos.service.CategoryService;
import com.pos.service.ProductService;
import com.pos.service.AuditLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/inventory")
public class InventoryController {
    @Autowired
    private ProductService productService;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private AuditLogService auditLogService;

    @GetMapping
    @PreAuthorize("hasRole('ADMINISTRATOR') or hasRole('INVENTORY_PERSONNEL')")
    public String showInventoryPage(Model model) {
        List<Product> products = productService.getActiveProducts();
        List<Category> categories = categoryService.getActiveCategories();
        
        System.out.println("Loading categories: " + categories.size() + " found");
        for (Category category : categories) {
            System.out.println("Category: " + category.getId() + " - " + category.getName());
        }
        
        model.addAttribute("products", products);
        model.addAttribute("categories", categories);
        
        return "inventory/index";
    }

    // Product API Endpoints
    @PostMapping("/api/products")
    @ResponseBody
    public ResponseEntity<Product> createProduct(@RequestBody Map<String, Object> payload) {
        Product product = new Product();
        product.setName((String) payload.get("name"));
        product.setDescription((String) payload.get("description"));
        product.setPrice(new BigDecimal(payload.get("price").toString()));
        product.setStockQuantity(Integer.parseInt(payload.get("stockQuantity").toString()));
        product.setVatable((Boolean) payload.get("vatable"));

        Long categoryId = Long.parseLong(payload.get("categoryId").toString());
        Category category = categoryService.getCategoryById(categoryId)
            .orElseThrow(() -> new RuntimeException("Category not found"));
        product.setCategory(category);

        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        Product created = productService.createProduct(product, username);
        return ResponseEntity.ok(created);
    }

    @PutMapping("/api/products/{id}")
    @ResponseBody
    public ResponseEntity<Product> updateProduct(@PathVariable Long id, @RequestBody Map<String, Object> payload) {
        Product existingProduct = productService.getProductById(id)
            .orElseThrow(() -> new RuntimeException("Product not found"));

        StringBuilder changes = new StringBuilder();
        if (!existingProduct.getName().equals(payload.get("name"))) {
            changes.append("Name: ").append(existingProduct.getName()).append(" -> ").append(payload.get("name")).append("; ");
        }
        if (!existingProduct.getDescription().equals(payload.get("description"))) {
            changes.append("Description changed; ");
        }
        if (!existingProduct.getPrice().equals(new BigDecimal(payload.get("price").toString()))) {
            changes.append("Price: ").append(existingProduct.getPrice()).append(" -> ").append(payload.get("price")).append("; ");
        }
        if (existingProduct.getVatable() != (Boolean) payload.get("vatable")) {
            changes.append("VAT status: ").append(existingProduct.getVatable() ? "VATable" : "Non-VATable")
                  .append(" -> ").append((Boolean) payload.get("vatable") ? "VATable" : "Non-VATable").append("; ");
        }

        Product product = new Product();
        product.setName((String) payload.get("name"));
        product.setDescription((String) payload.get("description"));
        product.setPrice(new BigDecimal(payload.get("price").toString()));
        product.setVatable((Boolean) payload.get("vatable"));

        Long categoryId = Long.parseLong(payload.get("categoryId").toString());
        Category category = categoryService.getCategoryById(categoryId)
            .orElseThrow(() -> new RuntimeException("Category not found"));
        product.setCategory(category);

        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        Product updated = productService.updateProduct(id, product, username);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/api/products/{id}")
    @ResponseBody
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        productService.deleteProduct(id, username);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/api/products/{id}/stock")
    @ResponseBody
    public ResponseEntity<Product> updateStock(@PathVariable Long id, @RequestBody Map<String, Object> payload) {
        int adjustment = Integer.parseInt(payload.get("adjustment").toString());
        String reason = payload.get("reason") != null ? (String) payload.get("reason") : "Manual adjustment";
        Product product = productService.getProductById(id)
            .orElseThrow(() -> new RuntimeException("Product not found"));

        int oldQuantity = product.getStockQuantity();
        int newQuantity = oldQuantity + adjustment;
        if (newQuantity < 0) {
            throw new RuntimeException("Insufficient stock");
        }

        product.setStockQuantity(newQuantity);
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        Product updated = productService.updateProduct(id, product, username);
        System.out.println("AUDIT: Logging stock adjustment for product " + product.getName() + ", old: " + oldQuantity + ", new: " + newQuantity + ", reason: " + reason);
        auditLogService.logStockAdjustment(username, product.getName(), product.getId(), 
            oldQuantity, newQuantity, reason);
        return ResponseEntity.ok(updated);
    }

    // Category API Endpoints
    @PostMapping("/api/categories")
    @ResponseBody
    public ResponseEntity<Category> createCategory(@RequestBody Category category) {
        Category created = categoryService.createCategory(category);
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        auditLogService.logCategoryCreation(username, created.getName(), created.getId());
        return ResponseEntity.ok(created);
    }

    @PutMapping("/api/categories/{id}")
    @ResponseBody
    public ResponseEntity<Category> updateCategory(@PathVariable Long id, @RequestBody Category category) {
        Category existingCategory = categoryService.getCategoryById(id)
            .orElseThrow(() -> new RuntimeException("Category not found"));

        StringBuilder changes = new StringBuilder();
        if (!existingCategory.getName().equals(category.getName())) {
            changes.append("Name: ").append(existingCategory.getName()).append(" -> ").append(category.getName()).append("; ");
        }
        if (!existingCategory.getDescription().equals(category.getDescription())) {
            changes.append("Description changed; ");
        }

        Category updated = categoryService.updateCategory(id, category);
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        auditLogService.logCategoryUpdate(username, updated.getName(), updated.getId(), changes.toString());
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/api/categories/{id}")
    @ResponseBody
    public ResponseEntity<Void> deleteCategory(@PathVariable Long id) {
        Category category = categoryService.getCategoryById(id)
            .orElseThrow(() -> new RuntimeException("Category not found"));
        categoryService.deleteCategory(id);
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        auditLogService.logCategoryDeletion(username, category.getName(), category.getId());
        return ResponseEntity.ok().build();
    }

    // Import/Export Endpoints
    @GetMapping("/api/products/template")
    public void downloadTemplate(HttpServletResponse response) throws Exception {
        response.setContentType("text/csv");
        response.setHeader("Content-Disposition", "attachment; filename=products_template.csv");

        try (CSVWriter writer = new CSVWriter(new OutputStreamWriter(response.getOutputStream()))) {
            writer.writeNext(new String[]{"Name", "Description", "Price", "Category ID", "Stock", "VATable"});
        }
    }

    @GetMapping("/api/products/export")
    public void exportProducts(HttpServletResponse response) throws Exception {
        response.setContentType("text/csv");
        response.setHeader("Content-Disposition", "attachment; filename=products.csv");

        List<Product> products = productService.getAllProducts();

        try (CSVWriter writer = new CSVWriter(new OutputStreamWriter(response.getOutputStream()))) {
            writer.writeNext(new String[]{"Name", "Description", "Price", "Category ID", "Stock", "VATable"});

            for (Product product : products) {
                writer.writeNext(new String[]{
                    product.getName(),
                    product.getDescription(),
                    product.getPrice().toString(),
                    product.getCategory().getId().toString(),
                    product.getStockQuantity().toString(),
                    String.valueOf(product.getVatable())
                });
            }
        }
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        auditLogService.logBulkExport(username, products.size(), "products.csv");
    }

    @PostMapping("/api/products/import")
    public ResponseEntity<Void> importProducts(@RequestParam("file") MultipartFile file) throws Exception {
        List<Product> importedProducts = new ArrayList<>();
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        try (CSVReader reader = new CSVReader(new InputStreamReader(file.getInputStream()))) {
            String[] line;
            reader.readNext(); // Skip header
            while ((line = reader.readNext()) != null) {
                Product product = new Product();
                product.setName(line[0]);
                product.setDescription(line[1]);
                product.setPrice(new BigDecimal(line[2]));
                product.setCategory(categoryService.getCategoryById(Long.parseLong(line[3]))
                    .orElseThrow(() -> new RuntimeException("Category not found")));
                product.setStockQuantity(Integer.parseInt(line[4]));
                product.setVatable(Boolean.parseBoolean(line[5]));
                importedProducts.add(productService.createProduct(product, username));
            }
        }
        auditLogService.logBulkImport(username, importedProducts.size(), file.getOriginalFilename());
        return ResponseEntity.ok().build();
    }
} 