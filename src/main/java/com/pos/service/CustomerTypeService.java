package com.pos.service;

import com.pos.entity.CustomerType;
import com.pos.repository.CustomerTypeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CustomerTypeService {
    @Autowired
    private CustomerTypeRepository customerTypeRepository;

    public List<CustomerType> getAllCustomerTypes() {
        return customerTypeRepository.findAll();
    }

    public java.util.Optional<CustomerType> getCustomerTypeById(Long id) {
        return customerTypeRepository.findById(id);
    }
} 