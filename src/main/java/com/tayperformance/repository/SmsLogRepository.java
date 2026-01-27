package com.tayperformance.repository;

import com.tayperformance.entity.SmsLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SmsLogRepository extends JpaRepository<SmsLog, Long> {
    List<SmsLog> findAllByAppointmentIdOrderByCreatedAtDesc(Long appointmentId);
}
