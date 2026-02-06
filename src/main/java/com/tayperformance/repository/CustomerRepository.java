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
 * Repository voor Customer.
 * Bevat lookup, zoeken, statistieken en GDPR queries.
 */
public interface CustomerRepository extends JpaRepository<Customer, Long> {

    // ============================================================
    // LOOKUP
    // ============================================================

    Optional<Customer> findByPhone(String phone);

    Optional<Customer> findByPhoneAndActiveTrue(String phone);

    boolean existsByPhone(String phone);

    boolean existsByPhoneAndActiveTrue(String phone);

    // ============================================================
    // LISTS & PAGINATION
    // ============================================================

    List<Customer> findAllByActiveTrueOrderByFirstNameAsc();

    Page<Customer> findAllByActiveTrueOrderByFirstNameAsc(Pageable pageable);

    // ============================================================
    // SEARCH
    // ============================================================

    @Query("""
        SELECT c FROM Customer c
        WHERE c.active = true
          AND (
               LOWER(c.phone) LIKE LOWER(CONCAT('%', :term, '%'))
            OR LOWER(c.firstName) LIKE LOWER(CONCAT('%', :term, '%'))
            OR LOWER(c.lastName) LIKE LOWER(CONCAT('%', :term, '%'))
          )
        ORDER BY c.firstName ASC, c.lastName ASC
    """)
    Page<Customer> searchActive(@Param("term") String term, Pageable pageable);

    // ============================================================
    // ANALYTICS
    // ============================================================

    @Query("""
        SELECT c FROM Customer c
        JOIN c.appointments a
        WHERE c.active = true
          AND a.status = 'COMPLETED'
        GROUP BY c
        ORDER BY COUNT(a) DESC
    """)
    List<Customer> findTopCustomers(Pageable pageable);

    @Query("""
        SELECT DISTINCT c FROM Customer c
        JOIN c.appointments a
        WHERE c.active = true
          AND a.startTime >= :since
        ORDER BY c.firstName ASC
    """)
    List<Customer> findActiveSince(@Param("since") OffsetDateTime since);

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

    @Query("""
        SELECT c FROM Customer c
        WHERE c.active = true
          AND c.createdAt < :cutoff
          AND NOT EXISTS (
                SELECT 1 FROM Appointment a
                WHERE a.customer.id = c.id
          )
        ORDER BY c.createdAt ASC
    """)
    List<Customer> findWithoutAppointmentsBefore(@Param("cutoff") OffsetDateTime cutoff);

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

    @Query("""
        SELECT c FROM Customer c
        JOIN c.appointments a
        WHERE c.active = true
          AND a.status = 'COMPLETED'
        GROUP BY c
        HAVING COUNT(a) >= :minCount
        ORDER BY COUNT(a) DESC
    """)
    List<Customer> findLoyalCustomers(@Param("minCount") long minCount);

    // ============================================================
    // STATS
    // ============================================================

    long countByActiveTrue();

    long countByActiveTrueAndCreatedAtBetween(OffsetDateTime start, OffsetDateTime end);
}