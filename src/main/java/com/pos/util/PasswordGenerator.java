package com.pos.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class PasswordGenerator {
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String rawPassword = "admin123";
        String encodedPassword = encoder.encode(rawPassword);
        
        System.out.println("Raw Password: " + rawPassword);
        System.out.println("Encoded Password: " + encodedPassword);
        
        // Verify the password
        boolean matches = encoder.matches(rawPassword, encodedPassword);
        System.out.println("Password Matches: " + matches);
        
        // Also verify against the stored hash
        String storedHash = "$2a$10$8.UnVuG9HHgffUDAlk8qfOuVGkqRzgVymGe07xd00DMxs.AQubh4a";
        boolean matchesStored = encoder.matches(rawPassword, storedHash);
        System.out.println("Matches Stored Hash: " + matchesStored);
    }
} 