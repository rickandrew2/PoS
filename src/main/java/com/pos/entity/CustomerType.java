package com.pos.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "customer_type")
public class CustomerType {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private boolean isPwd = false;

    @Column(nullable = false)
    private boolean isSenior = false;

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public boolean isPwd() { return isPwd; }
    public void setPwd(boolean pwd) { isPwd = pwd; }
    public boolean isSenior() { return isSenior; }
    public void setSenior(boolean senior) { isSenior = senior; }
} 