package com.tayperformance.repository;

import com.tayperformance.entity.Appointment;
import com.tayperformance.entity.AppointmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    // Soft check (geen lock). DB constraint is de echte guard.
    @Query("""
        SELECT a
        FROM Appointment a
        WHERE a.status IN :statuses
          AND (:staffId IS NULL OR a.assignedStaff.id = :staffId)
          AND a.startTime < :endTime
          AND a.endTime > :startTime
    """)
    List<Appointment> findConflicting(
            @Param("statuses") List<AppointmentStatus> statuses,
            @Param("staffId") Long staffId,
            @Param("startTime") OffsetDateTime startTime,
            @Param("endTime") OffsetDateTime endTime
    );

    List<Appointment> findAllByStartTimeBetweenOrderByStartTimeAsc(
            OffsetDateTime start, OffsetDateTime end
    );

    List<Appointment> findAllByCustomerIdOrderByStartTimeDesc(Long customerId);
}
