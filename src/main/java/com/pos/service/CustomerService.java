package com.pos.service;

import com.pos.entity.Customer;
import com.pos.repository.CustomerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
public class CustomerService {
    @Autowired
    private CustomerRepository customerRepository;

    public Customer createCustomer(Customer customer) {
        return customerRepository.save(customer);
    }

    public Customer updateCustomer(Long id, Customer customer) {
        Customer existingCustomer = customerRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Customer not found"));
        
        existingCustomer.setName(customer.getName());
        existingCustomer.setEmail(customer.getEmail());
        existingCustomer.setPhoneNumber(customer.getPhoneNumber());
        existingCustomer.setAddress(customer.getAddress());
        existingCustomer.setPwd(customer.isPwd());
        existingCustomer.setSenior(customer.isSenior());
        existingCustomer.setUpdatedAt(java.time.LocalDateTime.now());
        
        return customerRepository.save(existingCustomer);
    }

    public void deleteCustomer(Long id) {
        Customer customer = customerRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Customer not found"));
        customer.setActive(false);
        customer.setUpdatedAt(java.time.LocalDateTime.now());
        customerRepository.save(customer);
    }

    public List<Customer> getAllCustomers() {
        return customerRepository.findByActiveTrue();
    }

    public List<Customer> searchCustomers(String query) {
        return customerRepository.findByNameContainingIgnoreCaseAndActiveTrue(query);
    }

    public Optional<Customer> getCustomerById(Long id) {
        return customerRepository.findById(id);
    }

    public List<Customer> getActiveCustomers() {
        return customerRepository.findByActiveTrue();
    }
} 