package com.pos.controller;

import com.pos.entity.Customer;
import com.pos.entity.Transaction;
import com.pos.service.CustomerService;
import com.pos.service.TransactionService;
import com.pos.service.AuditLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/customers")
public class CustomerController {
    @Autowired
    private CustomerService customerService;

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private AuditLogService auditLogService;

    @GetMapping
    @PreAuthorize("hasRole('ADMINISTRATOR') or hasRole('CASHIER')")
    public String customersPage(Model model) {
        return "customers/index";
    }

    @GetMapping("/list")
    @ResponseBody
    @PreAuthorize("hasRole('ADMINISTRATOR') or hasRole('CASHIER')")
    public ResponseEntity<List<Customer>> getAllCustomers() {
        return ResponseEntity.ok(customerService.getActiveCustomers());
    }

    @GetMapping("/{id}")
    @ResponseBody
    @PreAuthorize("hasRole('ADMINISTRATOR') or hasRole('CASHIER')")
    public ResponseEntity<Customer> getCustomer(@PathVariable Long id) {
        return customerService.getCustomerById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @ResponseBody
    @PreAuthorize("hasRole('ADMINISTRATOR') or hasRole('CASHIER')")
    public ResponseEntity<Customer> createCustomer(@RequestBody Customer customer) {
        Customer created = customerService.createCustomer(customer);
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        auditLogService.logAction(username, "CREATE_CUSTOMER", "Created customer: " + created.getName());
        return ResponseEntity.ok(created);
    }

    @PutMapping("/{id}")
    @ResponseBody
    @PreAuthorize("hasRole('ADMINISTRATOR') or hasRole('CASHIER')")
    public ResponseEntity<Customer> updateCustomer(@PathVariable Long id, @RequestBody Customer customer) {
        Customer updated = customerService.updateCustomer(id, customer);
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        auditLogService.logAction(username, "UPDATE_CUSTOMER", "Updated customer: " + updated.getName());
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    @ResponseBody
    @PreAuthorize("hasRole('ADMINISTRATOR') or hasRole('CASHIER')")
    public ResponseEntity<Void> deleteCustomer(@PathVariable Long id) {
        customerService.deleteCustomer(id);
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        auditLogService.logAction(username, "DELETE_CUSTOMER", "Deleted customer with ID: " + id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/search")
    @ResponseBody
    @PreAuthorize("hasRole('ADMINISTRATOR') or hasRole('CASHIER')")
    public ResponseEntity<List<Customer>> searchCustomers(@RequestParam String query) {
        return ResponseEntity.ok(customerService.searchCustomers(query));
    }

    @GetMapping("/{id}/transactions")
    @ResponseBody
    @PreAuthorize("hasRole('ADMINISTRATOR') or hasRole('CASHIER')")
    public ResponseEntity<Map<String, Object>> getCustomerTransactions(@PathVariable Long id) {
        Customer customer = customerService.getCustomerById(id)
                .orElseThrow(() -> new RuntimeException("Customer not found"));
        
        List<Transaction> transactions = transactionService.getTransactionsByCustomer(id);
        
        Map<String, Object> response = new HashMap<>();
        response.put("customer", customer);
        response.put("transactions", transactions);
        
        return ResponseEntity.ok(response);
    }
} 