package com.pos.repository;

import com.pos.entity.StockRequest;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StockRequestRepository extends JpaRepository<StockRequest, Long> {
} 