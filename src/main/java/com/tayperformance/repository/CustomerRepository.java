package com.tayperformance.repository;

import com.tayperformance.entity.Customer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository voor Customer entiteit.
 * Bevat queries voor opzoeken, zoeken, analytics en GDPR compliance.
 */
public interface CustomerRepository extends JpaRepository<Customer, Long> {

    // ============================================================
    // BASIS OPVRAGEN
    // ============================================================

    /**
     * Vind klant op telefoonnummer (unieke business identifier).
     */
    Optional<Customer> findByPhone(String phone);

    /**
     * Vind actieve klant op telefoonnummer.
     */
    Optional<Customer> findByPhoneAndActiveTrue(String phone);

    /**
     * Check of telefoonnummer al bestaat (voor registratie validatie).
     */
    boolean existsByPhone(String phone);

    /**
     * Check of actieve klant met dit nummer bestaat.
     */
    boolean existsByPhoneAndActiveTrue(String phone);

    // ============================================================
    // LIJSTEN & PAGINERING
    // ============================================================

    /**
     * Alle actieve klanten gesorteerd op naam.
     */
    List<Customer> findAllByActiveTrueOrderByFirstNameAsc();

    /**
     * Gepagineerde lijst van actieve klanten.
     */
    Page<Customer> findAllByActiveTrueOrderByFirstNameAsc(Pageable pageable);

    // ============================================================
    // ZOEKEN
    // ============================================================

    /**
     * Zoek actieve klanten op telefoon, voornaam of achternaam.
     */
    @Query("""
        SELECT c FROM Customer c
        WHERE c.active = true
          AND (
            LOWER(c.phone) LIKE LOWER(CONCAT('%', :searchTerm, '%'))
            OR LOWER(c.firstName) LIKE LOWER(CONCAT('%', :searchTerm, '%'))
            OR LOWER(c.lastName) LIKE LOWER(CONCAT('%', :searchTerm, '%'))
          )
        ORDER BY c.firstName ASC, c.lastName ASC
    """)
    List<Customer> searchActive(@Param("searchTerm") String searchTerm);

    /**
     * Gepagineerde zoekresultaten.
     */
    @Query("""
        SELECT c FROM Customer c
        WHERE c.active = true
          AND (
            LOWER(c.phone) LIKE LOWER(CONCAT('%', :searchTerm, '%'))
            OR LOWER(c.firstName) LIKE LOWER(CONCAT('%', :searchTerm, '%'))
            OR LOWER(c.lastName) LIKE LOWER(CONCAT('%', :searchTerm, '%'))
          )
        ORDER BY c.firstName ASC, c.lastName ASC
    """)
    Page<Customer> searchActive(@Param("searchTerm") String searchTerm, Pageable pageable);

    // ============================================================
    // ANALYTICS & RAPPORTAGE
    // ============================================================

    /**
     * Top klanten (meeste completed afspraken).
     * Gebruik Pageable om top N te krijgen: PageRequest.of(0, 10)
     */
    @Query("""
        SELECT c FROM Customer c
        JOIN c.appointments a
        WHERE c.active = true
          AND a.status = 'COMPLETED'
        GROUP BY c
        ORDER BY COUNT(a) DESC
    """)
    List<Customer> findTopCustomers(Pageable pageable);

    /**
     * Actieve klanten sinds datum (voor retention analyse).
     */
    @Query("""
        SELECT DISTINCT c FROM Customer c
        JOIN c.appointments a
        WHERE c.active = true
          AND a.startTime >= :since
        ORDER BY c.firstName ASC
    """)
    List<Customer> findActiveSince(@Param("since") OffsetDateTime since);

    /**
     * Inactieve klanten (geen afspraken sinds datum).
     * NOT EXISTS voorkomt N+1 query probleem.
     */
    @Query("""
        SELECT c FROM Customer c
        WHERE c.active = true
          AND NOT EXISTS (
            SELECT 1 FROM Appointment a
            WHERE a.customer.id = c.id
              AND a.startTime >= :since
          )
        ORDER BY c.firstName ASC
    """)
    List<Customer> findInactiveSince(@Param("since") OffsetDateTime since);

    /**
     * Klanten zonder enige afspraak (voor opschoning).
     * Typisch: cutoffDate = now().minusMonths(6)
     */
    @Query("""
        SELECT c FROM Customer c
        WHERE c.active = true
          AND c.createdAt < :cutoffDate
          AND NOT EXISTS (
            SELECT 1 FROM Appointment a 
            WHERE a.customer.id = c.id
          )
        ORDER BY c.createdAt ASC
    """)
    List<Customer> findWithoutAppointmentsBefore(@Param("cutoffDate") OffsetDateTime cutoffDate);

    /**
     * Klanten met meerdere no-shows (voor flagging/blocking).
     */
    @Query("""
        SELECT c FROM Customer c
        JOIN c.appointments a
        WHERE c.active = true
          AND a.status = 'NOSHOW'
          AND a.startTime >= :since
        GROUP BY c
        HAVING COUNT(a) >= :minNoShows
        ORDER BY COUNT(a) DESC
    """)
    List<Customer> findWithMultipleNoShows(
            @Param("since") OffsetDateTime since,
            @Param("minNoShows") long minNoShows
    );

    /**
     * Loyale klanten (voor korting eligibility).
     * minCompletedCount = typisch 4
     */
    @Query("""
        SELECT c FROM Customer c
        JOIN c.appointments a
        WHERE c.active = true
          AND a.status = 'COMPLETED'
        GROUP BY c
        HAVING COUNT(a) >= :minCompletedCount
        ORDER BY COUNT(a) DESC
    """)
    List<Customer> findLoyalCustomers(@Param("minCompletedCount") long minCompletedCount);

    // ============================================================
    // STATISTIEKEN
    // ============================================================

    /**
     * Tel actieve klanten.
     */
    long countByActiveTrue();

    /**
     * Nieuwe klanten per periode.
     */
    long countByActiveTrueAndCreatedAtBetween(OffsetDateTime start, OffsetDateTime end);
}