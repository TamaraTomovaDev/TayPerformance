package com.tayperformance.repository;

import com.tayperformance.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    List<AuditLog> findAllByEntityTypeAndEntityIdOrderByCreatedAtDesc(String entityType, Long entityId);
}