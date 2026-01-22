package com.tayperformance.repository;

import com.tayperformance.entity.Appointment;
import com.tayperformance.entity.AppointmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    /**
     * CONFLICT CHECK (NIEUWE AFSPRAAK)
     * Controleert of er een overlappende afspraak bestaat
     * die NIET geannuleerd is.
     * Regel:
     * bestaand.start < nieuw.einde
     * EN
     * bestaand.einde > nieuw.start
     */
    boolean existsByStartTimeLessThanAndEndTimeGreaterThanAndStatusNot(
            LocalDateTime endTime,
            LocalDateTime startTime,
            AppointmentStatus status
    );

    /**
     * CONFLICT CHECK (BESTAANDE AFSPRAAK WIJZIGEN)
     * Zelfde als hierboven, maar sluit de huidige afspraak uit
     * (belangrijk bij edit).
     */
    @Query("""
        SELECT COUNT(a) > 0
        FROM Appointment a
        WHERE a.id <> :appointmentId
          AND a.status <> :status
          AND a.startTime < :endTime
          AND a.endTime > :startTime
    """)
    boolean existsConflictExcludingAppointment(
            @Param("appointmentId") Long appointmentId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            @Param("status") AppointmentStatus status
    );

    /**
     * DAGOVERZICHT
     * Alle afspraken voor één dag (kalender)
     */
    List<Appointment> findAllByStartTimeBetweenOrderByStartTimeAsc(
            LocalDateTime dayStart,
            LocalDateTime dayEnd
    );

    /**
     * TOEKOMSTIGE AFSPRAKEN (optioneel)
     */
    List<Appointment> findAllByStartTimeAfterOrderByStartTimeAsc(
            LocalDateTime from
    );
}
