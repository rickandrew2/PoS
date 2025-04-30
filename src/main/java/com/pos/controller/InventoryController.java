package com.pos.controller;

import com.pos.entity.Category;
import com.pos.entity.Product;
import com.pos.service.CategoryService;
import com.pos.service.ProductService;
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

        return ResponseEntity.ok(productService.createProduct(product));
    }

    @PutMapping("/api/products/{id}")
    @ResponseBody
    public ResponseEntity<Product> updateProduct(@PathVariable Long id, @RequestBody Map<String, Object> payload) {
        Product product = productService.getProductById(id)
            .orElseThrow(() -> new RuntimeException("Product not found"));

        product.setName((String) payload.get("name"));
        product.setDescription((String) payload.get("description"));
        product.setPrice(new BigDecimal(payload.get("price").toString()));
        product.setVatable((Boolean) payload.get("vatable"));

        Long categoryId = Long.parseLong(payload.get("categoryId").toString());
        Category category = categoryService.getCategoryById(categoryId)
            .orElseThrow(() -> new RuntimeException("Category not found"));
        product.setCategory(category);

        return ResponseEntity.ok(productService.updateProduct(id, product));
    }

    @DeleteMapping("/api/products/{id}")
    @ResponseBody
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
        productService.deleteProduct(id);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/api/products/{id}/stock")
    @ResponseBody
    public ResponseEntity<Product> updateStock(@PathVariable Long id, @RequestBody Map<String, Object> payload) {
        int adjustment = Integer.parseInt(payload.get("adjustment").toString());
        Product product = productService.getProductById(id)
            .orElseThrow(() -> new RuntimeException("Product not found"));

        int newStock = product.getStockQuantity() + adjustment;
        if (newStock < 0) {
            throw new RuntimeException("Insufficient stock");
        }

        product.setStockQuantity(newStock);
        return ResponseEntity.ok(productService.updateProduct(id, product));
    }

    // Category API Endpoints
    @PostMapping("/api/categories")
    @ResponseBody
    public ResponseEntity<Category> createCategory(@RequestBody Category category) {
        return ResponseEntity.ok(categoryService.createCategory(category));
    }

    @PutMapping("/api/categories/{id}")
    @ResponseBody
    public ResponseEntity<Category> updateCategory(@PathVariable Long id, @RequestBody Category category) {
        return ResponseEntity.ok(categoryService.updateCategory(id, category));
    }

    @DeleteMapping("/api/categories/{id}")
    @ResponseBody
    public ResponseEntity<Void> deleteCategory(@PathVariable Long id) {
        categoryService.deleteCategory(id);
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
    }

    @PostMapping("/api/products/import")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> importProducts(@RequestParam("file") MultipartFile file) throws Exception {
        List<Product> products = new ArrayList<>();

        try (CSVReader reader = new CSVReader(new InputStreamReader(file.getInputStream()))) {
            String[] header = reader.readNext(); // Skip header
            String[] line;
            while ((line = reader.readNext()) != null) {
                final String[] currentLine = line;
                Product product = new Product();
                product.setName(currentLine[0]);
                product.setDescription(currentLine[1]);
                product.setPrice(new BigDecimal(currentLine[2]));
                
                Category category = categoryService.getCategoryById(Long.parseLong(currentLine[3]))
                    .orElseThrow(() -> new RuntimeException("Category not found: " + currentLine[3]));
                product.setCategory(category);
                
                product.setStockQuantity(Integer.parseInt(currentLine[4]));
                product.setVatable(Boolean.parseBoolean(currentLine[5]));
                
                products.add(product);
            }
        }

        for (Product product : products) {
            productService.createProduct(product);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("imported", products.size());
        return ResponseEntity.ok(response);
    }
} 