package com.tayperformance.repository;

import com.tayperformance.entity.Appointment;
import com.tayperformance.entity.AppointmentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository voor Appointment entiteit.
 * Bevat queries voor conflict detectie, kalender, analytics en archivering.
 */
public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    // ============================================================
    // CONFLICT DETECTIE
    // ============================================================

    /**
     * Vindt overlappende afspraken voor dubbele boeking preventie.
     * Overlap = (start1 < end2) AND (end1 > start2)
     */
    @Query("""
        SELECT a FROM Appointment a
        WHERE a.status IN :statuses
          AND (:staffId IS NULL OR a.assignedStaff.id = :staffId)
          AND a.startTime < :endTime
          AND a.endTime > :startTime
          AND (:excludeId IS NULL OR a.id != :excludeId)
        ORDER BY a.startTime ASC
    """)
    List<Appointment> findConflicting(
            @Param("statuses") List<AppointmentStatus> statuses,
            @Param("staffId") Long staffId,
            @Param("startTime") OffsetDateTime startTime,
            @Param("endTime") OffsetDateTime endTime,
            @Param("excludeId") Long excludeId
    );

    // ============================================================
    // KALENDER QUERIES
    // ============================================================

    /**
     * Afspraken in periode (week/maand view).
     */
    @Query("""
        SELECT a FROM Appointment a
        WHERE a.startTime >= :start
          AND a.startTime < :end
        ORDER BY a.startTime ASC
    """)
    List<Appointment> findByDateRange(
            @Param("start") OffsetDateTime start,
            @Param("end") OffsetDateTime end
    );

    /**
     * Afspraken voor specifieke dag.
     * Service layer: dayStart = date.atStartOfDay(zoneId)
     */
    @Query("""
        SELECT a FROM Appointment a
        WHERE a.startTime >= :dayStart
          AND a.startTime < :dayEnd
        ORDER BY a.startTime ASC
    """)
    List<Appointment> findByDay(
            @Param("dayStart") OffsetDateTime dayStart,
            @Param("dayEnd") OffsetDateTime dayEnd
    );

    /**
     * Afspraken voor specifieke staff member in periode.
     */
    @Query("""
        SELECT a FROM Appointment a
        WHERE a.assignedStaff.id = :staffId
          AND a.startTime >= :start
          AND a.startTime < :end
        ORDER BY a.startTime ASC
    """)
    List<Appointment> findByStaffAndDateRange(
            @Param("staffId") Long staffId,
            @Param("start") OffsetDateTime start,
            @Param("end") OffsetDateTime end
    );

    /**
     * Afspraken vandaag voor dashboard.
     */
    @Query("""
        SELECT a FROM Appointment a
        WHERE a.startTime >= :todayStart
          AND a.startTime < :tomorrowStart
          AND a.status IN ('CONFIRMED', 'IN_PROGRESS')
        ORDER BY a.startTime ASC
    """)
    List<Appointment> findTodaysAppointments(
            @Param("todayStart") OffsetDateTime todayStart,
            @Param("tomorrowStart") OffsetDateTime tomorrowStart
    );

    // ============================================================
    // STATUS FILTERING
    // ============================================================

    List<Appointment> findAllByStatusOrderByStartTimeDesc(AppointmentStatus status);

    List<Appointment> findAllByStatusInOrderByStartTimeDesc(List<AppointmentStatus> statuses);

    /**
     * Pending confirmations voor dashboard alert.
     */
    @Query("""
        SELECT a FROM Appointment a
        WHERE a.status = 'REQUESTED'
        ORDER BY a.createdAt ASC
    """)
    List<Appointment> findPendingConfirmation();

    /**
     * Status statistieken voor rapportage.
     */
    @Query("""
        SELECT a.status as status, COUNT(a) as count
        FROM Appointment a
        WHERE a.startTime >= :startDate 
          AND a.startTime < :endDate
        GROUP BY a.status
    """)
    List<StatusCount> countByStatusBetweenDates(
            @Param("startDate") OffsetDateTime startDate,
            @Param("endDate") OffsetDateTime endDate
    );

    // ============================================================
    // KLANTGESCHIEDENIS
    // ============================================================

    /**
     * Alle afspraken van klant (voor history view).
     */
    List<Appointment> findAllByCustomerIdOrderByStartTimeDesc(Long customerId);

    /**
     * Completed afspraken sinds datum (voor loyaliteit berekening).
     */
    @Query("""
        SELECT a FROM Appointment a
        WHERE a.customer.id = :customerId
          AND a.status = 'COMPLETED'
          AND a.startTime >= :since
        ORDER BY a.startTime DESC
    """)
    List<Appointment> findCompletedByCustomerSince(
            @Param("customerId") Long customerId,
            @Param("since") OffsetDateTime since
    );

    /**
     * Tel completed afspraken (loyaliteitskorting na 4 afspraken).
     */
    @Query("""
        SELECT COUNT(a) FROM Appointment a
        WHERE a.customer.id = :customerId
          AND a.status = 'COMPLETED'
    """)
    long countCompletedByCustomer(@Param("customerId") Long customerId);

    /**
     * Tel no-shows (voor klant blocking na 3 no-shows).
     */
    @Query("""
        SELECT COUNT(a) FROM Appointment a
        WHERE a.customer.id = :customerId
          AND a.status = 'NOSHOW'
          AND a.startTime >= :since
    """)
    long countNoShowsByCustomerSince(
            @Param("customerId") Long customerId,
            @Param("since") OffsetDateTime since
    );

    // ============================================================
    // SMS REMINDERS
    // ============================================================

    /**
     * Afspraken die reminder nodig hebben (batch job).
     * Window = typically 23-25 uur voor afspraak.
     */
    @Query("""
        SELECT a FROM Appointment a
        WHERE a.status = 'CONFIRMED'
          AND a.startTime >= :windowStart
          AND a.startTime < :windowEnd
        ORDER BY a.startTime ASC
    """)
    List<Appointment> findAppointmentsForReminder(
            @Param("windowStart") OffsetDateTime windowStart,
            @Param("windowEnd") OffsetDateTime windowEnd
    );

    // ============================================================
    // ANALYTICS
    // ============================================================

    /**
     * Completed afspraken met prijs voor omzet rapportage.
     */
    @Query("""
        SELECT a FROM Appointment a
        WHERE a.status = 'COMPLETED'
          AND a.price IS NOT NULL
          AND a.startTime >= :startDate
          AND a.startTime < :endDate
        ORDER BY a.startTime ASC
    """)
    List<Appointment> findCompletedWithPriceBetweenDates(
            @Param("startDate") OffsetDateTime startDate,
            @Param("endDate") OffsetDateTime endDate
    );

    /**
     * Totale omzet berekening (BigDecimal voor nauwkeurigheid).
     */
    @Query("""
        SELECT COALESCE(SUM(a.price), 0)
        FROM Appointment a
        WHERE a.status = 'COMPLETED'
          AND a.price IS NOT NULL
          AND a.startTime >= :startDate
          AND a.startTime < :endDate
    """)
    BigDecimal calculateRevenueBetweenDates(
            @Param("startDate") OffsetDateTime startDate,
            @Param("endDate") OffsetDateTime endDate
    );

    /**
     * Meest populaire services voor rapportage.
     */
    @Query("""
        SELECT s.name as serviceName, COUNT(a) as appointmentCount
        FROM Appointment a
        JOIN a.service s
        WHERE a.status = 'COMPLETED'
          AND a.startTime >= :startDate
          AND a.startTime < :endDate
        GROUP BY s.name
        ORDER BY COUNT(a) DESC
    """)
    List<ServicePopularity> findMostPopularServices(
            @Param("startDate") OffsetDateTime startDate,
            @Param("endDate") OffsetDateTime endDate
    );

    /**
     * Gemiddelde duur per service voor planning optimalisatie.
     */
    @Query("""
        SELECT s.name as serviceName, AVG(EXTRACT(EPOCH FROM (a.endTime - a.startTime))/60) as avgMinutes
        FROM Appointment a
        JOIN a.service s
        WHERE a.status = 'COMPLETED'
          AND a.startTime >= :startDate
          AND a.startTime < :endDate
        GROUP BY s.name
        ORDER BY AVG(EXTRACT(EPOCH FROM (a.endTime - a.startTime))/60) DESC
    """)
    List<ServiceDuration> calculateAverageDurationByService(
            @Param("startDate") OffsetDateTime startDate,
            @Param("endDate") OffsetDateTime endDate
    );

    // ============================================================
    // ZOEKEN & PAGINERING
    // ============================================================

    Page<Appointment> findAllByOrderByStartTimeDesc(Pageable pageable);

    /**
     * Zoek afspraken op klantgegevens.
     */
    @Query("""
        SELECT a FROM Appointment a
        WHERE LOWER(a.customer.phone) LIKE LOWER(CONCAT('%', :searchTerm, '%'))
           OR LOWER(a.customer.firstName) LIKE LOWER(CONCAT('%', :searchTerm, '%'))
           OR LOWER(a.customer.lastName) LIKE LOWER(CONCAT('%', :searchTerm, '%'))
        ORDER BY a.startTime DESC
    """)
    Page<Appointment> searchByCustomer(
            @Param("searchTerm") String searchTerm,
            Pageable pageable
    );

    /**
     * Zoek afspraken op auto merk/model.
     */
    @Query("""
        SELECT a FROM Appointment a
        WHERE LOWER(a.carBrand) LIKE LOWER(CONCAT('%', :searchTerm, '%'))
           OR LOWER(a.carModel) LIKE LOWER(CONCAT('%', :searchTerm, '%'))
        ORDER BY a.startTime DESC
    """)
    Page<Appointment> searchByCar(
            @Param("searchTerm") String searchTerm,
            Pageable pageable
    );

    // ============================================================
    // ONDERHOUD / ARCHIVERING
    // ============================================================

    /**
     * Oude afspraken voor archivering (batch processing met paginering).
     * Typisch: cutoffDate = now().minusMonths(12)
     */
    @Query("""
        SELECT a FROM Appointment a
        WHERE a.startTime < :cutoffDate
          AND a.status IN ('COMPLETED', 'CANCELED', 'NOSHOW')
        ORDER BY a.startTime ASC
    """)
    Page<Appointment> findOldAppointmentsForArchival(
            @Param("cutoffDate") OffsetDateTime cutoffDate,
            Pageable pageable
    );

    /**
     * Vind upcoming afspraken zonder toegewezen staff.
     */
    @Query("""
        SELECT a FROM Appointment a
        WHERE a.assignedStaff IS NULL
          AND a.status IN ('REQUESTED', 'CONFIRMED')
          AND a.startTime > :now
        ORDER BY a.startTime ASC
    """)
    List<Appointment> findUnassignedUpcoming(@Param("now") OffsetDateTime now);
}

// ============================================================
// PROJECTION INTERFACES
// ============================================================

interface StatusCount {
    AppointmentStatus getStatus();
    Long getCount();
}

interface ServicePopularity {
    String getServiceName();
    Long getAppointmentCount();
}

interface ServiceDuration {
    String getServiceName();
    Double getAvgMinutes();
}