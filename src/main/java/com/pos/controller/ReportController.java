package com.pos.controller;

import com.pos.service.TransactionService;
import com.pos.entity.Transaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/reports")
public class ReportController {

    @Autowired
    private TransactionService transactionService;

    @GetMapping
    @PreAuthorize("hasRole('ADMINISTRATOR') or hasRole('CASHIER')")
    public String showReportsPage(Model model) {
        // Get today's date range
        LocalDate today = LocalDate.now();
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = today.plusDays(1).atStartOfDay();
        Map<String, Object> todayReport = transactionService.generateSalesReport(startOfDay, endOfDay);

        // Get this week's date range
        LocalDate startOfWeek = today.with(TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY));
        LocalDate endOfWeek = startOfWeek.plusDays(6);
        LocalDateTime startOfWeekDateTime = startOfWeek.atStartOfDay();
        LocalDateTime endOfWeekDateTime = endOfWeek.plusDays(1).atStartOfDay();
        Map<String, Object> weeklyReport = transactionService.generateSalesReport(startOfWeekDateTime, endOfWeekDateTime);

        model.addAttribute("todayReport", todayReport);
        model.addAttribute("weeklyReport", weeklyReport);
        model.addAttribute("today", today);
        model.addAttribute("startOfWeek", startOfWeek);
        model.addAttribute("endOfWeek", endOfWeek);

        return "reports/index";
    }

    @GetMapping("/sales/daily")
    @PreAuthorize("hasRole('ADMINISTRATOR') or hasRole('CASHIER')")
    public String showDailySalesReport(
            @RequestParam(required = false) String date,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search,
            Model model) {
        LocalDate reportDate = date != null ? LocalDate.parse(date) : LocalDate.now();
        LocalDateTime startOfDay = reportDate.atStartOfDay();
        LocalDateTime endOfDay = reportDate.plusDays(1).atStartOfDay();

        Map<String, Object> report = transactionService.generateSalesReport(startOfDay, endOfDay);
        List<Transaction> transactions = (List<Transaction>) report.get("transactions");

        // Filter by search
        if (search != null && !search.isEmpty()) {
            String searchLower = search.toLowerCase();
            transactions = transactions.stream().filter(t ->
                (t.getReceiptNumber() != null && t.getReceiptNumber().toLowerCase().contains(searchLower)) ||
                (t.getCustomerType() != null && t.getCustomerType().getTitle().toLowerCase().contains(searchLower)) ||
                (t.getTransactionDate() != null && t.getTransactionDate().toString().toLowerCase().contains(searchLower))
            ).collect(Collectors.toList());
        }

        // Pagination
        int totalTransactions = transactions.size();
        int totalPages = (int) Math.ceil((double) totalTransactions / size);
        int fromIndex = Math.max(0, (page - 1) * size);
        int toIndex = Math.min(fromIndex + size, totalTransactions);
        List<Transaction> pagedTransactions = transactions.subList(fromIndex, toIndex);

        report.put("transactions", pagedTransactions);
        model.addAttribute("report", report);
        model.addAttribute("reportDate", reportDate);
        model.addAttribute("reportType", "Daily");
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("search", search);
        model.addAttribute("size", size);

        return "reports/sales-report";
    }

    @GetMapping("/sales/weekly")
    @PreAuthorize("hasRole('ADMINISTRATOR') or hasRole('CASHIER')")
    public String showWeeklySalesReport(
            @RequestParam(required = false) String startDate,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search,
            Model model) {
        LocalDate reportStartDate = startDate != null ? LocalDate.parse(startDate) : LocalDate.now().with(TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY));
        LocalDate reportEndDate = reportStartDate.plusDays(6);
        LocalDateTime startOfWeek = reportStartDate.atStartOfDay();
        LocalDateTime endOfWeek = reportEndDate.plusDays(1).atStartOfDay();

        Map<String, Object> report = transactionService.generateSalesReport(startOfWeek, endOfWeek);
        List<Transaction> transactions = (List<Transaction>) report.get("transactions");

        // Filter by search
        if (search != null && !search.isEmpty()) {
            String searchLower = search.toLowerCase();
            transactions = transactions.stream().filter(t ->
                (t.getReceiptNumber() != null && t.getReceiptNumber().toLowerCase().contains(searchLower)) ||
                (t.getCustomerType() != null && t.getCustomerType().getTitle().toLowerCase().contains(searchLower)) ||
                (t.getTransactionDate() != null && t.getTransactionDate().toString().toLowerCase().contains(searchLower))
            ).collect(Collectors.toList());
        }

        // Pagination
        int totalTransactions = transactions.size();
        int totalPages = (int) Math.ceil((double) totalTransactions / size);
        int fromIndex = Math.max(0, (page - 1) * size);
        int toIndex = Math.min(fromIndex + size, totalTransactions);
        List<Transaction> pagedTransactions = transactions.subList(fromIndex, toIndex);

        report.put("transactions", pagedTransactions);
        model.addAttribute("report", report);
        model.addAttribute("reportStartDate", reportStartDate);
        model.addAttribute("reportEndDate", reportEndDate);
        model.addAttribute("reportType", "Weekly");
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("search", search);
        model.addAttribute("size", size);

        return "reports/sales-report";
    }
} 