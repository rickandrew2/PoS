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
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

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
        System.out.println("AUDIT: Logging stock adjustment for product " + product.getName() + ", old: " + oldQuantity + ", new: " + newQuantity + ", reason: " + reason);
        auditLogService.logStockAdjustment(username, product.getName(), product.getId(), 
            oldQuantity, newQuantity, reason);
        return ResponseEntity.ok(product);
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

    // Helper to generate PDF and return as byte array
    private byte[] generateProductsPdf(List<Product> products) throws Exception {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.LETTER);
            document.addPage(page);
            PDPageContentStream contentStream = new PDPageContentStream(document, page);

            float margin = 40;
            float yStart = page.getMediaBox().getHeight() - margin;
            float tableWidth = page.getMediaBox().getWidth() - 2 * margin;
            float yPosition = yStart;
            float rowHeight = 24;
            float[] colWidths = {80, 160, 60, 70, 50, 60};
            String[] headers = {"Name", "Description", "Price", "Category ID", "Stock", "VATable"};

            // Title
            contentStream.setFont(PDType1Font.HELVETICA_BOLD, 16);
            contentStream.beginText();
            contentStream.newLineAtOffset(margin, yPosition);
            contentStream.showText("Product Inventory Export");
            contentStream.endText();
            yPosition -= rowHeight;

            // Draw header background
            contentStream.setNonStrokingColor(220, 220, 220);
            contentStream.addRect(margin, yPosition, tableWidth, rowHeight);
            contentStream.fill();
            contentStream.setNonStrokingColor(0, 0, 0);

            // Draw table header
            contentStream.setFont(PDType1Font.HELVETICA_BOLD, 11);
            float xPosition = margin;
            for (int i = 0; i < headers.length; i++) {
                contentStream.beginText();
                contentStream.newLineAtOffset(xPosition + 2, yPosition + 6);
                contentStream.showText(headers[i]);
                contentStream.endText();
                xPosition += colWidths[i];
            }
            yPosition -= rowHeight;

            // Draw table rows
            contentStream.setFont(PDType1Font.HELVETICA, 10);
            for (Product product : products) {
                xPosition = margin;
                String[] row = new String[]{
                    product.getName(),
                    product.getDescription(),
                    product.getPrice().toString(),
                    product.getCategory().getId().toString(),
                    product.getStockQuantity().toString(),
                    String.valueOf(product.getVatable())
                };
                for (int i = 0; i < row.length; i++) {
                    String cell = row[i] != null ? row[i] : "";
                    // Wrap description if too long
                    if (i == 1 && cell.length() > 40) {
                        cell = cell.substring(0, 37) + "...";
                    }
                    contentStream.beginText();
                    contentStream.newLineAtOffset(xPosition + 2, yPosition + 6);
                    contentStream.showText(cell);
                    contentStream.endText();
                    xPosition += colWidths[i];
                }
                yPosition -= rowHeight;
                // Draw row borders
                contentStream.moveTo(margin, yPosition + rowHeight);
                contentStream.lineTo(margin + tableWidth, yPosition + rowHeight);
                contentStream.stroke();
                if (yPosition < margin + rowHeight) {
                    contentStream.close();
                    page = new PDPage(PDRectangle.LETTER);
                    document.addPage(page);
                    contentStream = new PDPageContentStream(document, page);
                    yPosition = yStart;
                }
            }
            // Draw column borders
            xPosition = margin;
            for (float w : colWidths) {
                contentStream.moveTo(xPosition, yStart);
                contentStream.lineTo(xPosition, yPosition);
                contentStream.stroke();
                xPosition += w;
            }
            // Draw right border
            contentStream.moveTo(margin + tableWidth, yStart);
            contentStream.lineTo(margin + tableWidth, yPosition);
            contentStream.stroke();
            // Draw bottom border
            contentStream.moveTo(margin, yPosition);
            contentStream.lineTo(margin + tableWidth, yPosition);
            contentStream.stroke();
            contentStream.close();
            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
            document.save(baos);
            return baos.toByteArray();
        }
    }

    @GetMapping("/api/products/export")
    public void exportProducts(HttpServletResponse response) throws Exception {
        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename=products.pdf");
        List<Product> products = productService.getAllProducts();
        byte[] pdfBytes = generateProductsPdf(products);
        response.getOutputStream().write(pdfBytes);
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        auditLogService.logBulkExport(username, products.size(), "products.pdf");
    }

    @GetMapping(value = "/api/products/export/preview", produces = "application/pdf")
    @ResponseBody
    public byte[] previewProductsPdf() throws Exception {
        List<Product> products = productService.getAllProducts();
        return generateProductsPdf(products);
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

    @GetMapping("/api/products/export/excel")
    public void exportProductsExcel(HttpServletResponse response) throws Exception {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=products.xlsx");

        List<Product> products = productService.getAllProducts();

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Products");
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("ID");
            header.createCell(1).setCellValue("Name");
            header.createCell(2).setCellValue("Category");
            header.createCell(3).setCellValue("Price");
            header.createCell(4).setCellValue("Stock");
            header.createCell(5).setCellValue("VATable");

            int rowIdx = 1;
            for (Product product : products) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(product.getId());
                row.createCell(1).setCellValue(product.getName());
                row.createCell(2).setCellValue(product.getCategory().getName());
                row.createCell(3).setCellValue(product.getPrice().doubleValue());
                row.createCell(4).setCellValue(product.getStockQuantity());
                row.createCell(5).setCellValue(product.getVatable() != null && product.getVatable() ? "Yes" : "No");
            }

            for (int i = 0; i <= 5; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(response.getOutputStream());
        }
    }
} 