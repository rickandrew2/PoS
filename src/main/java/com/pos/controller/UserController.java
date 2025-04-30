package com.pos.controller;

import com.pos.dto.UserDTO;
import com.pos.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/users")
public class UserController {

    private final UserService userService;

    @Autowired
    public UserController(UserService userService) {
        this.userService = userService;
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
        return userService.createUser(userDTO);
    }

    @PutMapping("/api/users/{id}")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    @ResponseBody
    public UserDTO updateUser(@PathVariable Long id, @RequestBody UserDTO userDTO) {
        userDTO.setId(id);
        return userService.updateUser(userDTO);
    }

    @DeleteMapping("/api/users/{id}")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    @ResponseBody
    public void deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
    }
} 