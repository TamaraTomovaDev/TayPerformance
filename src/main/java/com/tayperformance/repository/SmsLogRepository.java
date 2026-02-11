package com.tayperformance.repository;

import com.tayperformance.entity.SmsLog;
import com.tayperformance.entity.SmsStatus;
import com.tayperformance.entity.SmsType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;

public interface SmsLogRepository extends JpaRepository<SmsLog, Long> {

    // ✅ bestaande (dup-prevent)
    @Query("""
        SELECT CASE WHEN COUNT(s) > 0 THEN true ELSE false END
        FROM SmsLog s
        WHERE s.appointment.id = :appointmentId
          AND s.type = :type
          AND s.status IN ('SENT', 'DELIVERED')
    """)
    boolean hasTypeBeenSent(@Param("appointmentId") Long appointmentId, @Param("type") SmsType type);

    // ✅ nodig: logs per afspraak
    List<SmsLog> findAllByAppointmentIdOrderByCreatedAtDesc(Long appointmentId);

    // ✅ handig: filter op status
    Page<SmsLog> findAllByStatusOrderByCreatedAtDesc(SmsStatus status, Pageable pageable);

    // ✅ handig: failed sinds X dagen (troubleshooting)
    @Query("""
        SELECT s FROM SmsLog s
        WHERE s.status = 'FAILED'
          AND s.createdAt >= :since
        ORDER BY s.createdAt DESC
    """)
    Page<SmsLog> findFailedSince(@Param("since") OffsetDateTime since, Pageable pageable);
}
