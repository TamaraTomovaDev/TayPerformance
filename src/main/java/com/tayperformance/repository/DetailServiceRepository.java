package com.tayperformance.repository;

import com.tayperformance.entity.DetailService;
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
 * Repository voor DetailService entiteit.
 * Bevat queries voor service management, populariteit en analytics.
 */
public interface DetailServiceRepository extends JpaRepository<DetailService, Long> {

    // ============================================================
    // BASIS OPHALEN
    // ============================================================

    /**
     * Alle actieve services gesorteerd op naam.
     */
    List<DetailService> findAllByActiveTrueOrderByNameAsc();

    /**
     * Gepagineerde lijst van actieve services.
     */
    Page<DetailService> findAllByActiveTrueOrderByNameAsc(Pageable pageable);

    /**
     * Vind actieve service op naam.
     */
    Optional<DetailService> findByNameAndActiveTrue(String name);

    /**
     * Vind service op naam (ook inactieve).
     */
    Optional<DetailService> findByName(String name);

    /**
     * Check of service naam al bestaat.
     */
    boolean existsByName(String name);

    /**
     * Check of actieve service met naam bestaat.
     */
    boolean existsByNameAndActiveTrue(String name);

    // ============================================================
    // POPULAIRE SERVICES
    // ============================================================

    /**
     * Meest populaire services (meeste completed afspraken).
     */
    @Query("""
        SELECT s FROM DetailService s
        JOIN Appointment a ON a.service.id = s.id
        WHERE a.status = 'COMPLETED' 
          AND a.startTime >= :since
        GROUP BY s
        ORDER BY COUNT(a) DESC
    """)
    List<DetailService> findMostPopularSince(@Param("since") OffsetDateTime since);

    /**
     * Gepagineerde populaire services.
     */
    @Query("""
        SELECT s FROM DetailService s
        JOIN Appointment a ON a.service.id = s.id
        WHERE a.status = 'COMPLETED' 
          AND a.startTime >= :since
        GROUP BY s
        ORDER BY COUNT(a) DESC
    """)
    Page<DetailService> findMostPopularSince(@Param("since") OffsetDateTime since, Pageable pageable);

    /**
     * Service populariteit met aantal afspraken.
     */
    @Query("""
        SELECT s.name as serviceName, 
               COUNT(a) as appointmentCount,
               s.active as active
        FROM DetailService s
        LEFT JOIN Appointment a ON a.service.id = s.id
            AND a.status = 'COMPLETED'
            AND a.startTime >= :since
        GROUP BY s.id, s.name, s.active
        ORDER BY COUNT(a) DESC
    """)
    List<ServicePopularityStats> getPopularityStats(@Param("since") OffsetDateTime since);

    // ============================================================
    // ANALYTICS
    // ============================================================

    /**
     * Services met omzet statistieken.
     */
    @Query("""
        SELECT s.name as serviceName,
               COUNT(a) as appointmentCount,
               COALESCE(SUM(a.price), 0) as totalRevenue,
               COALESCE(AVG(a.price), 0) as avgPrice
        FROM DetailService s
        LEFT JOIN Appointment a ON a.service.id = s.id
            AND a.status = 'COMPLETED'
            AND a.price IS NOT NULL
            AND a.startTime >= :since
        WHERE s.active = true
        GROUP BY s.id, s.name
        ORDER BY COALESCE(SUM(a.price), 0) DESC
    """)
    List<ServiceRevenueStats> getRevenueStats(@Param("since") OffsetDateTime since);

    /**
     * Services met gemiddelde duur statistieken.
     */
    @Query("""
        SELECT s.name as serviceName,
               s.defaultMinutes as plannedMinutes,
               AVG(EXTRACT(EPOCH FROM (a.endTime - a.startTime))/60) as actualAvgMinutes,
               COUNT(a) as sampleSize
        FROM DetailService s
        LEFT JOIN Appointment a ON a.service.id = s.id
            AND a.status = 'COMPLETED'
            AND a.startTime >= :since
        WHERE s.active = true
        GROUP BY s.id, s.name, s.defaultMinutes
        HAVING COUNT(a) > 0
        ORDER BY s.name ASC
    """)
    List<ServiceDurationStats> getDurationStats(@Param("since") OffsetDateTime since);

    /**
     * Services zonder bookings (ongebruikte services).
     */
    @Query("""
        SELECT s FROM DetailService s
        WHERE s.active = true
          AND NOT EXISTS (
            SELECT 1 FROM Appointment a
            WHERE a.service.id = s.id
              AND a.startTime >= :since
          )
        ORDER BY s.name ASC
    """)
    List<DetailService> findUnusedSince(@Param("since") OffsetDateTime since);

    // ============================================================
    // ZOEKEN
    // ============================================================

    /**
     * Zoek actieve services op naam of beschrijving.
     */
    @Query("""
        SELECT s FROM DetailService s
        WHERE s.active = true
          AND (
            LOWER(s.name) LIKE LOWER(CONCAT('%', :searchTerm, '%'))
            OR LOWER(s.description) LIKE LOWER(CONCAT('%', :searchTerm, '%'))
          )
        ORDER BY s.name ASC
    """)
    List<DetailService> searchActive(@Param("searchTerm") String searchTerm);

    /**
     * Gepagineerde zoekresultaten.
     */
    @Query("""
        SELECT s FROM DetailService s
        WHERE s.active = true
          AND (
            LOWER(s.name) LIKE LOWER(CONCAT('%', :searchTerm, '%'))
            OR LOWER(s.description) LIKE LOWER(CONCAT('%', :searchTerm, '%'))
          )
        ORDER BY s.name ASC
    """)
    Page<DetailService> searchActive(@Param("searchTerm") String searchTerm, Pageable pageable);

    // ============================================================
    // FILTERING OP DUUR
    // ============================================================

    /**
     * Services die passen in tijdsvenster (voor booking suggesties).
     */
    @Query("""
        SELECT s FROM DetailService s
        WHERE s.active = true
          AND s.minMinutes <= :availableMinutes
        ORDER BY s.defaultMinutes DESC
    """)
    List<DetailService> findFittingInTimeWindow(@Param("availableMinutes") int availableMinutes);

    /**
     * Snelle services (max 60 min).
     */
    @Query("""
        SELECT s FROM DetailService s
        WHERE s.active = true
          AND s.maxMinutes <= 60
        ORDER BY s.defaultMinutes ASC
    """)
    List<DetailService> findQuickServices();

    /**
     * Lange services (min 120 min).
     */
    @Query("""
        SELECT s FROM DetailService s
        WHERE s.active = true
          AND s.minMinutes >= 120
        ORDER BY s.defaultMinutes DESC
    """)
    List<DetailService> findExtensiveServices();

    // ============================================================
    // STATISTIEKEN
    // ============================================================

    /**
     * Tel actieve services.
     */
    long countByActiveTrue();

    /**
     * Tel alle services (inclusief inactieve).
     */
    long count();
}

// ============================================================
// PROJECTION INTERFACES
// ============================================================

interface ServicePopularityStats {
    String getServiceName();
    Long getAppointmentCount();
    Boolean getActive();
}

interface ServiceRevenueStats {
    String getServiceName();
    Long getAppointmentCount();
    BigDecimal getTotalRevenue();
    BigDecimal getAvgPrice();
}

interface ServiceDurationStats {
    String getServiceName();
    Integer getPlannedMinutes();
    Double getActualAvgMinutes();
    Long getSampleSize();
}