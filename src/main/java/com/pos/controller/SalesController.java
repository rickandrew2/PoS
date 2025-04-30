package com.pos.controller;

import com.pos.entity.Category;
import com.pos.entity.Customer;
import com.pos.entity.Product;
import com.pos.service.CategoryService;
import com.pos.service.CustomerService;
import com.pos.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequestMapping("/sales")
public class SalesController {
    @Autowired
    private ProductService productService;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private CustomerService customerService;

    @GetMapping
    @PreAuthorize("hasRole('ADMINISTRATOR') or hasRole('CASHIER')")
    public String showSalesPage(Model model) {
        List<Product> products = productService.getActiveProducts();
        List<Category> categories = categoryService.getActiveCategories();
        List<Customer> customers = customerService.getActiveCustomers();

        model.addAttribute("products", products);
        model.addAttribute("categories", categories);
        model.addAttribute("customers", customers);

        return "sales/index";
    }
} 