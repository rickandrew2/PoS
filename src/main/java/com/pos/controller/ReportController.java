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
import java.util.*;
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

    @GetMapping("/sales")
    @PreAuthorize("hasRole('ADMINISTRATOR') or hasRole('CASHIER')")
    public String showSalesReport(
            @RequestParam(defaultValue = "daily") String type,
            @RequestParam(required = false) String date,
            @RequestParam(required = false) String week,
            @RequestParam(required = false) String month,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Model model) {

        LocalDateTime start = null, end = null;
        String reportType = "Daily";

        if ("daily".equalsIgnoreCase(type)) {
            reportType = "Daily";
            LocalDate reportDate = (date != null && !date.isEmpty()) ? LocalDate.parse(date) : LocalDate.now();
            start = reportDate.atStartOfDay();
            end = reportDate.plusDays(1).atStartOfDay();
            model.addAttribute("reportDate", reportDate);
        } else if ("weekly".equalsIgnoreCase(type)) {
            reportType = "Weekly";
            // week format: "2025-W19"
            LocalDate weekStart = (week != null && !week.isEmpty())
                ? LocalDate.parse(week + "-1", java.time.format.DateTimeFormatter.ofPattern("YYYY-'W'ww-e"))
                : LocalDate.now().with(java.time.DayOfWeek.MONDAY);
            LocalDate weekEnd = weekStart.plusDays(6);
            start = weekStart.atStartOfDay();
            end = weekEnd.plusDays(1).atStartOfDay();
            model.addAttribute("reportStartDate", weekStart);
            model.addAttribute("reportEndDate", weekEnd);
            model.addAttribute("reportWeek", week);
        } else if ("monthly".equalsIgnoreCase(type)) {
            reportType = "Monthly";
            // month format: "2025-05"
            LocalDate monthStart = (month != null && !month.isEmpty())
                ? LocalDate.parse(month + "-01")
                : LocalDate.now().withDayOfMonth(1);
            LocalDate monthEnd = monthStart.withDayOfMonth(monthStart.lengthOfMonth());
            start = monthStart.atStartOfDay();
            end = monthEnd.plusDays(1).atStartOfDay();
            model.addAttribute("reportMonth", month);
            model.addAttribute("reportStartDate", monthStart);
            model.addAttribute("reportEndDate", monthEnd);
        }

        Map<String, Object> report = transactionService.generateSalesReport(start, end);
        model.addAttribute("report", report);
        model.addAttribute("reportType", reportType);

        // PAGINATION: Get paged transactions for the table
        org.springframework.data.domain.Page<Transaction> transactionsPage = transactionService.getTransactionsByDateRangePaged(start, end, org.springframework.data.domain.PageRequest.of(page, size, org.springframework.data.domain.Sort.by("transactionDate").descending()));
        model.addAttribute("transactionsPage", transactionsPage);

        // For charts and stats, use the full list
        List<Transaction> transactions = (List<Transaction>) report.get("transactions");

        // 1. Sales Trend Over Time
        Map<String, Double> salesTrend = new LinkedHashMap<>();
        List<String> salesTrendLabels = new ArrayList<>();
        List<Double> salesTrendValues = new ArrayList<>();
        if ("Daily".equals(reportType)) {
            // Group by hour for daily
            Map<Integer, Double> hourlySales = new TreeMap<>();
            for (int i = 0; i < 24; i++) hourlySales.put(i, 0.0);
            for (Transaction t : transactions) {
                int hour = t.getTransactionDate().getHour();
                hourlySales.put(hour, hourlySales.get(hour) + t.getTotalAmount().doubleValue());
            }
            for (int i = 0; i < 24; i++) {
                salesTrendLabels.add(String.format("%02d:00", i));
                salesTrendValues.add(hourlySales.get(i));
            }
        } else if ("Weekly".equals(reportType)) {
            // Group by day for weekly
            LocalDate weekStart = (LocalDate) model.getAttribute("reportStartDate");
            for (int i = 0; i < 7; i++) {
                LocalDate day = weekStart.plusDays(i);
                salesTrendLabels.add(day.toString());
                double sum = transactions.stream()
                    .filter(t -> t.getTransactionDate().toLocalDate().equals(day))
                    .mapToDouble(t -> t.getTotalAmount().doubleValue()).sum();
                salesTrendValues.add(sum);
            }
        } else if ("Monthly".equals(reportType)) {
            // Group by day for monthly
            LocalDate monthStart = (LocalDate) model.getAttribute("reportStartDate");
            LocalDate monthEnd = (LocalDate) model.getAttribute("reportEndDate");
            for (LocalDate day = monthStart; !day.isAfter(monthEnd); day = day.plusDays(1)) {
                final LocalDate currentDay = day;
                salesTrendLabels.add(currentDay.toString());
                double sum = transactions.stream()
                    .filter(t -> t.getTransactionDate().toLocalDate().equals(currentDay))
                    .mapToDouble(t -> t.getTotalAmount().doubleValue()).sum();
                salesTrendValues.add(sum);
            }
        }
        for (int i = 0; i < salesTrendLabels.size(); i++) {
            salesTrend.put(salesTrendLabels.get(i), salesTrendValues.get(i));
        }
        Map<String, Object> salesTrendData = new HashMap<>();
        salesTrendData.put("labels", salesTrendLabels);
        salesTrendData.put("data", salesTrendValues);
        model.addAttribute("salesTrend", salesTrendData);
        // Add zipped rows for Google Charts
        List<List<Object>> salesTrendRows = new ArrayList<>();
        for (int i = 0; i < salesTrendLabels.size(); i++) {
            salesTrendRows.add(Arrays.asList(salesTrendLabels.get(i), salesTrendValues.get(i)));
        }
        model.addAttribute("salesTrendRows", salesTrendRows);

        // 2. Sales by Category
        Map<String, Double> categorySales = new HashMap<>();
        for (Transaction t : transactions) {
            if (t.getItems() != null) {
                for (var item : t.getItems()) {
                    String category = item.getProduct().getCategory().getName();
                    double subtotal = item.getSubtotal().doubleValue();
                    categorySales.merge(category, subtotal, Double::sum);
                }
            }
        }

        List<Map.Entry<String, Double>> sortedCategories = categorySales.entrySet().stream()
            .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
            .collect(Collectors.toList());

        Map<String, Object> categoryData = new HashMap<>();
        List<String> categoryLabels = sortedCategories.stream().map(Map.Entry::getKey).collect(Collectors.toList());
        List<Double> categoryValues = sortedCategories.stream().map(Map.Entry::getValue).collect(Collectors.toList());
        categoryData.put("labels", categoryLabels);
        categoryData.put("data", categoryValues);
        model.addAttribute("categorySales", categoryData);
        // Add zipped rows for Google Charts
        List<List<Object>> categoryRows = new ArrayList<>();
        for (int i = 0; i < categoryLabels.size(); i++) {
            categoryRows.add(Arrays.asList(categoryLabels.get(i), categoryValues.get(i)));
        }
        model.addAttribute("categoryRows", categoryRows);

        // 3. Top Products
        Map<String, Double> productSales = new HashMap<>();
        for (Transaction t : transactions) {
            if (t.getItems() != null) {
                for (var item : t.getItems()) {
                    String product = item.getProduct().getName();
                    double subtotal = item.getSubtotal().doubleValue();
                    productSales.merge(product, subtotal, Double::sum);
                }
            }
        }

        List<Map.Entry<String, Double>> topProducts = productSales.entrySet().stream()
            .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
            .limit(5)
            .collect(Collectors.toList());

        Map<String, Object> topProductsData = new HashMap<>();
        List<String> topProductLabels = topProducts.stream().map(Map.Entry::getKey).collect(Collectors.toList());
        List<Double> topProductValues = topProducts.stream().map(Map.Entry::getValue).collect(Collectors.toList());
        topProductsData.put("labels", topProductLabels);
        topProductsData.put("data", topProductValues);
        model.addAttribute("topProducts", topProductsData);
        // Add zipped rows for Google Charts
        List<List<Object>> topProductsRows = new ArrayList<>();
        for (int i = 0; i < topProductLabels.size(); i++) {
            topProductsRows.add(Arrays.asList(topProductLabels.get(i), topProductValues.get(i)));
        }
        model.addAttribute("topProductsRows", topProductsRows);

        return "reports/sales-report";
    }
} 