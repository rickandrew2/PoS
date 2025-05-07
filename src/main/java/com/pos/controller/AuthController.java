package com.pos.controller;

import com.pos.dto.UserDTO;
import com.pos.model.User;
import com.pos.service.UserService;
import com.pos.service.RecaptchaService;
import com.pos.security.JwtService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {
    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private UserService userService;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private RecaptchaService recaptchaService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        try {
            logger.debug("Login attempt for user: {}", request.getUsername());

            // Authenticate user (remove reCAPTCHA check)
            Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
            );

            // Get user details
            User user = userService.getUserByUsername(request.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

            // Generate JWT token
            String token = jwtService.generateToken((UserDetails) authentication.getPrincipal());

            // Update last login time
            userService.updateLastLogin(user.getUsername());

            logger.info("User {} successfully logged in", user.getUsername());

            return ResponseEntity.ok(new LoginResponse(token, user));
        } catch (BadCredentialsException e) {
            logger.warn("Invalid credentials for user: {}", request.getUsername());
            return ResponseEntity.badRequest().body(createErrorResponse("Invalid username or password"));
        } catch (Exception e) {
            logger.error("Login error for user {}: {}", request.getUsername(), e.getMessage(), e);
            return ResponseEntity.badRequest().body(createErrorResponse("An error occurred during login"));
        }
    }

    private Map<String, String> createErrorResponse(String message) {
        Map<String, String> response = new HashMap<>();
        response.put("error", message);
        return response;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody UserDTO userDTO) {
        try {
            UserDTO createdUser = userService.createUser(userDTO);
            logger.info("New user registered: {}", createdUser.getUsername());
            return ResponseEntity.ok(createdUser);
        } catch (Exception e) {
            logger.error("Registration error: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        }
    }
}

class LoginRequest {
    private String username;
    private String password;
    private String recaptchaToken;

    // Getters and setters
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getRecaptchaToken() { return recaptchaToken; }
    public void setRecaptchaToken(String recaptchaToken) { this.recaptchaToken = recaptchaToken; }
}

class LoginResponse {
    private String token;
    private User user;

    public LoginResponse(String token, User user) {
        this.token = token;
        this.user = user;
    }

    // Getters and setters
    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
} 