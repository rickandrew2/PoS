package com.pos.controller;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import com.pos.service.TransactionService;
import com.pos.service.ProductService;
import com.pos.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import java.math.BigDecimal;

@Controller
public class DashboardController {

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private ProductService productService;

    @Autowired
    private UserService userService;

    @GetMapping("/")
    public String root() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !auth.getName().equals("anonymousUser")) {
            return "redirect:/dashboard";
        }
        return "redirect:/login";
    }

    @GetMapping("/dashboard")
    public String showDashboard(Model model) {
        // Get today's sales
        BigDecimal todaysSales = transactionService.getTodaysSales();
        model.addAttribute("todaysSales", todaysSales);

        // Get total products
        long totalProducts = productService.getTotalProducts();
        model.addAttribute("totalProducts", totalProducts);

        // Get total users
        long totalUsers = userService.getTotalUsers();
        model.addAttribute("totalUsers", totalUsers);

        return "dashboard";
    }
} 