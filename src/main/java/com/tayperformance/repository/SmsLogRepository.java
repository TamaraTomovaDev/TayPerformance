package com.tayperformance.repository;

import com.tayperformance.entity.SmsLog;
import com.tayperformance.entity.SmsStatus;
import com.tayperformance.entity.SmsType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;

public interface SmsLogRepository extends JpaRepository<SmsLog, Long> {
    void deleteByAppointment_Id(Long appointmentId);

    @Query("""
        SELECT (COUNT(s) > 0)
        FROM SmsLog s
        WHERE s.appointment.id = :appointmentId
          AND s.type = :type
          AND s.status IN (
             com.tayperformance.entity.SmsStatus.QUEUED,
             com.tayperformance.entity.SmsStatus.SENT,
             com.tayperformance.entity.SmsStatus.DELIVERED
          )
    """)
    boolean hasTypeBeenSent(@Param("appointmentId") Long appointmentId,
                            @Param("type") SmsType type);

    // logs per afspraak
    List<SmsLog> findAllByAppointment_IdOrderByCreatedAtDesc(Long appointmentId);

    // filter op status (paginatie)
    Page<SmsLog> findAllByStatusOrderByCreatedAtDesc(SmsStatus status, Pageable pageable);

    // failed sinds X dagen (paginatie)
    @Query("""
        SELECT s FROM SmsLog s
        WHERE s.status = com.tayperformance.entity.SmsStatus.FAILED
          AND s.createdAt >= :since
        ORDER BY s.createdAt DESC
    """)
    Page<SmsLog> findFailedSince(@Param("since") OffsetDateTime since, Pageable pageable);
}
