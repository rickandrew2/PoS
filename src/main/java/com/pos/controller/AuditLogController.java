package com.pos.controller;

import com.pos.repository.AuditLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin/audit-log")
@PreAuthorize("hasRole('ADMINISTRATOR')")
public class AuditLogController {
    @Autowired
    private AuditLogRepository auditLogRepository;

    @GetMapping
    public String viewAuditLog(Model model) {
        model.addAttribute("logs", auditLogRepository.findAll());
        return "admin/audit-log";
    }
} 