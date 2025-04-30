package com.pos.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class CustomerDTO {
    private Long id;
    private String name;
    private String email;
    private String phoneNumber;
    private String address;
    private Boolean isPwd;
    private Boolean isSenior;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Boolean active;
} 