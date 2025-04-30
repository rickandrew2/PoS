package com.pos.repository;

import com.pos.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CustomerRepository extends JpaRepository<Customer, Long> {
    List<Customer> findByActiveTrue();
    List<Customer> findByNameContainingIgnoreCaseAndActiveTrue(String name);
    List<Customer> findByPhoneNumberContainingAndActiveTrue(String phoneNumber);
    List<Customer> findByEmailContainingIgnoreCaseAndActiveTrue(String email);
    boolean existsByEmail(String email);
    boolean existsByPhoneNumber(String phoneNumber);
} 