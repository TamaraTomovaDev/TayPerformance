package com.tayperformance.repository;

import com.tayperformance.entity.Appointment;
import com.tayperformance.entity.AppointmentStatus;
import com.tayperformance.dto.projection.ServiceDuration;
import com.tayperformance.dto.projection.ServicePopularity;
import com.tayperformance.dto.projection.StatusCount;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * Repository voor Appointment entiteit.
 */
public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    // ============================================================
    // CONFLICT DETECTIE
    // ============================================================

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

    @Query("""
        SELECT a FROM Appointment a
        WHERE a.status = 'REQUESTED'
        ORDER BY a.createdAt ASC
    """)
    List<Appointment> findPendingConfirmation();

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

    List<Appointment> findAllByCustomerIdOrderByStartTimeDesc(Long customerId);

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

    @Query("""
        SELECT COUNT(a) FROM Appointment a
        WHERE a.customer.id = :customerId
          AND a.status = 'COMPLETED'
    """)
    long countCompletedByCustomer(@Param("customerId") Long customerId);

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

    @Query("""
    SELECT s.name AS serviceName,
           AVG(timestampdiff(MINUTE, a.startTime, a.endTime)) AS avgMinutes
    FROM Appointment a
    JOIN a.service s
    WHERE a.status = 'COMPLETED'
      AND a.startTime >= :startDate
      AND a.startTime < :endDate
    GROUP BY s.name
    ORDER BY avgMinutes DESC
   """)
    List<ServiceDuration> calculateAverageDurationByService(
            @Param("startDate") OffsetDateTime startDate,
            @Param("endDate") OffsetDateTime endDate
    );

    // ============================================================
    // ZOEKEN & PAGINERING
    // ============================================================

    Page<Appointment> findAllByOrderByStartTimeDesc(Pageable pageable);

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

    @Query("""
        SELECT a FROM Appointment a
        WHERE a.assignedStaff IS NULL
          AND a.status IN ('REQUESTED', 'CONFIRMED')
          AND a.startTime > :now
        ORDER BY a.startTime ASC
    """)
    List<Appointment> findUnassignedUpcoming(@Param("now") OffsetDateTime now);
}