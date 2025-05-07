package com.pos.controller;

import com.pos.entity.Category;
import com.pos.entity.Customer;
import com.pos.entity.Product;
import com.pos.entity.Transaction;
import com.pos.entity.TransactionItem;
import com.pos.entity.CustomerType;
import com.pos.model.User;
import com.pos.security.CustomUserDetails;
import com.pos.service.CategoryService;
import com.pos.service.CustomerService;
import com.pos.service.ProductService;
import com.pos.service.TransactionService;
import com.pos.service.CustomerTypeService;
import com.pos.service.AuditLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.HashMap;

@Controller
@RequestMapping("/sales")
public class SalesController {
    @Autowired
    private ProductService productService;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private CustomerService customerService;

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private CustomerTypeService customerTypeService;

    @Autowired
    private AuditLogService auditLogService;

    @GetMapping
    @PreAuthorize("hasRole('ADMINISTRATOR') or hasRole('CASHIER')")
    public String showSalesPage(Model model) {
        List<Product> products = productService.getActiveProducts();
        List<Category> categories = categoryService.getActiveCategories();
        List<CustomerType> customerTypeList = customerTypeService.getAllCustomerTypes();

        model.addAttribute("products", products);
        model.addAttribute("categories", categories);
        model.addAttribute("customerTypeList", customerTypeList);

        return "sales/index";
    }

    @GetMapping("/receipt/{receiptNumber}")
    @PreAuthorize("hasRole('ADMINISTRATOR') or hasRole('CASHIER')")
    public String viewReceipt(@PathVariable String receiptNumber, Model model) {
        Transaction transaction = transactionService.getTransactionByReceiptNumber(receiptNumber)
            .orElseThrow(() -> new RuntimeException("Receipt not found: " + receiptNumber));
        
        model.addAttribute("transaction", transaction);
        return "sales/receipt";
    }

    @GetMapping("/receipt/{receiptNumber}/print")
    @PreAuthorize("hasRole('ADMINISTRATOR') or hasRole('CASHIER')")
    public String printReceipt(@PathVariable String receiptNumber, Model model) {
        Transaction transaction = transactionService.getTransactionByReceiptNumber(receiptNumber)
            .orElseThrow(() -> new RuntimeException("Receipt not found: " + receiptNumber));
        
        model.addAttribute("transaction", transaction);
        return "sales/receipt-print";
    }

    @PostMapping(value = "/process", produces = "application/json")
    @PreAuthorize("hasRole('ADMINISTRATOR') or hasRole('CASHIER')")
    @ResponseBody
    public ResponseEntity<?> processTransaction(@RequestBody Map<String, Object> payload) {
        try {
            Transaction transaction = new Transaction();
            
            // Set the current user as cashier
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            CustomUserDetails userDetails = (CustomUserDetails) auth.getPrincipal();
            User cashier = userDetails.getUser();
            transaction.setCashier(cashier);

            // Set customer type if provided
            Object customerTypeIdObj = payload.get("customerTypeId");
            if (customerTypeIdObj != null) {
                Long customerTypeId = Long.parseLong(customerTypeIdObj.toString());
                CustomerType customerType = customerTypeService.getCustomerTypeById(customerTypeId)
                    .orElseThrow(() -> new RuntimeException("Customer type not found"));
                transaction.setCustomerType(customerType);
            }

            // Set VAT and discount flags
            transaction.setVatIncluded((Boolean) payload.get("vatIncluded"));
            transaction.setPwdDiscount(payload.get("isPwdDiscount") != null && (Boolean) payload.get("isPwdDiscount"));
            transaction.setSeniorDiscount(payload.get("isSeniorDiscount") != null && (Boolean) payload.get("isSeniorDiscount"));

            // Process items
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> items = (List<Map<String, Object>>) payload.get("items");
            List<TransactionItem> transactionItems = items.stream()
                .map(item -> {
                    TransactionItem transactionItem = new TransactionItem();
                    Product product = productService.getProductById(Long.parseLong(item.get("productId").toString()))
                        .orElseThrow(() -> new RuntimeException("Product not found"));
                    
                    transactionItem.setProduct(product);
                    transactionItem.setQuantity(Integer.parseInt(item.get("quantity").toString()));
                    transactionItem.setUnitPrice(product.getPrice());
                    transactionItem.setTransaction(transaction);
                    return transactionItem;
                })
                .collect(Collectors.toList());

            transaction.setItems(transactionItems);

            // Create the transaction
            Transaction savedTransaction = transactionService.createTransaction(transaction);
            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            auditLogService.logAction(username, "PROCESS_TRANSACTION", "Processed transaction, receipt: " + savedTransaction.getReceiptNumber());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Transaction processed successfully");
            response.put("receiptNumber", savedTransaction.getReceiptNumber());
            response.put("totalAmount", savedTransaction.getTotalAmount());
            response.put("receiptUrl", "/sales/receipt/" + savedTransaction.getReceiptNumber());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }
} 