package com.pos.controller;

import com.pos.repository.AuditLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Controller
@RequestMapping("/admin/audit-log")
@PreAuthorize("hasRole('ADMINISTRATOR')")
public class AuditLogController {
    @Autowired
    private AuditLogRepository auditLogRepository;

    @GetMapping
    public String viewAuditLog(
            @RequestParam(value = "action", required = false) String action,
            @RequestParam(value = "startDate", required = false) String startDate,
            @RequestParam(value = "endDate", required = false) String endDate,
            Model model) {
        List logs;
        LocalDateTime start = null;
        LocalDateTime end = null;
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        if (startDate != null && !startDate.isEmpty()) {
            start = LocalDate.parse(startDate, dtf).atStartOfDay();
        }
        if (endDate != null && !endDate.isEmpty()) {
            end = LocalDate.parse(endDate, dtf).atTime(23, 59, 59);
        }
        if (action != null && !action.isEmpty() && start != null && end != null) {
            logs = auditLogRepository.findByActionAndTimestampBetweenOrderByTimestampDesc(action, start, end);
        } else if (action != null && !action.isEmpty()) {
            logs = auditLogRepository.findByActionOrderByTimestampDesc(action);
        } else if (start != null && end != null) {
            logs = auditLogRepository.findByTimestampBetweenOrderByTimestampDesc(start, end);
        } else {
            logs = auditLogRepository.findAllByOrderByTimestampDesc();
        }
        // Preprocess details for clickable receipt links
        Pattern receiptPattern = Pattern.compile("receipt: (\\d+)");
        List<AuditLogHtml> logsHtml = (List<AuditLogHtml>) logs.stream().map(log -> {
            com.pos.entity.AuditLog auditLog = (com.pos.entity.AuditLog) log;
            String details = auditLog.getDetails();
            Matcher matcher = receiptPattern.matcher(details);
            String htmlDetails = details;
            if (matcher.find()) {
                String receiptNumber = matcher.group(1);
                String link = String.format("<a href='/sales/receipt/%s' target='_blank'>%s</a>", receiptNumber, receiptNumber);
                htmlDetails = matcher.replaceFirst("receipt: " + link);
            }
            return new AuditLogHtml(auditLog, htmlDetails);
        }).collect(Collectors.toList());
        // Get all distinct actions for the filter dropdown
        List<String> actions = auditLogRepository.findDistinctActions();
        model.addAttribute("logs", logsHtml);
        model.addAttribute("actions", actions);
        model.addAttribute("selectedAction", action);
        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate", endDate);
        return "admin/audit-log";
    }

    // Helper DTO for HTML details
    public static class AuditLogHtml {
        private final com.pos.entity.AuditLog log;
        private final String htmlDetails;
        public AuditLogHtml(com.pos.entity.AuditLog log, String htmlDetails) {
            this.log = log;
            this.htmlDetails = htmlDetails;
        }
        public String getHtmlDetails() { return htmlDetails; }
        public java.time.LocalDateTime getTimestamp() { return log.getTimestamp(); }
        public String getUsername() { return log.getUsername(); }
        public String getAction() { return log.getAction(); }
    }
} 