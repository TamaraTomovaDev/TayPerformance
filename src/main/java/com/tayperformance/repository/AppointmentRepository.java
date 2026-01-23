package com.tayperformance.repository;

import com.tayperformance.entity.Appointment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.time.OffsetDateTime; // Zorg dat je de juiste import hebt
import java.util.List;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT a
        FROM Appointment a
        WHERE a.status <> 'CANCELED'
          AND a.startTime < :endTime
          AND a.endTime > :startTime
    """)
    List<Appointment> findConflictingAppointments(
            @Param("startTime") OffsetDateTime startTime, // Veranderd naar OffsetDateTime
            @Param("endTime") OffsetDateTime endTime     // Veranderd naar OffsetDateTime
    );

    // Vergeet deze ook niet aan te passen naar OffsetDateTime
    List<Appointment> findAllByStartTimeBetweenOrderByStartTimeAsc(
            OffsetDateTime start, OffsetDateTime end
    );
}
