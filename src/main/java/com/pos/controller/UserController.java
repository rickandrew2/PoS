package com.pos.controller;

import com.pos.dto.UserDTO;
import com.pos.service.UserService;
import com.pos.service.AuditLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/users")
public class UserController {

    private final UserService userService;
    private final AuditLogService auditLogService;

    @Autowired
    public UserController(UserService userService, AuditLogService auditLogService) {
        this.userService = userService;
        this.auditLogService = auditLogService;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public String listUsers(Model model) {
        List<UserDTO> users = userService.getAllUsers();
        model.addAttribute("users", users);
        return "users/index";
    }

    @PostMapping("/api/users")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    @ResponseBody
    public UserDTO createUser(@RequestBody UserDTO userDTO) {
        UserDTO created = userService.createUser(userDTO);
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        auditLogService.logAction(username, "CREATE_USER", "Created user: " + created.getUsername());
        return created;
    }

    @PutMapping("/api/users/{id}")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    @ResponseBody
    public UserDTO updateUser(@PathVariable Long id, @RequestBody UserDTO userDTO) {
        userDTO.setId(id);
        UserDTO updated = userService.updateUser(userDTO);
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        auditLogService.logAction(username, "UPDATE_USER", "Updated user: " + updated.getUsername());
        return updated;
    }

    @DeleteMapping("/api/users/{id}")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    @ResponseBody
    public void deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        auditLogService.logAction(username, "DELETE_USER", "Deleted user with ID: " + id);
    }
} 