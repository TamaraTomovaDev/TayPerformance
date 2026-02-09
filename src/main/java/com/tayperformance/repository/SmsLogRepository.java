package com.tayperformance.repository;

import com.tayperformance.entity.SmsLog;
import com.tayperformance.entity.SmsType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SmsLogRepository extends JpaRepository<SmsLog, Long> {

    @Query("""
        SELECT CASE WHEN COUNT(s) > 0 THEN true ELSE false END
        FROM SmsLog s
        WHERE s.appointment.id = :appointmentId
          AND s.type = :type
          AND s.status IN ('SENT', 'DELIVERED')
    """)
    boolean hasTypeBeenSent(@Param("appointmentId") Long appointmentId, @Param("type") SmsType type);
}
