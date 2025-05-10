package com.pos.repository;

import com.pos.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    List<AuditLog> findByUsernameOrderByTimestampDesc(String username);
    List<AuditLog> findAllByOrderByTimestampDesc();
    List<AuditLog> findByActionOrderByTimestampDesc(String action);
    List<AuditLog> findByTimestampBetweenOrderByTimestampDesc(java.time.LocalDateTime start, java.time.LocalDateTime end);
    List<AuditLog> findByActionAndTimestampBetweenOrderByTimestampDesc(String action, java.time.LocalDateTime start, java.time.LocalDateTime end);
    @Query("SELECT DISTINCT a.action FROM AuditLog a")
    List<String> findDistinctActions();
} 