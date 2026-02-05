package com.tayperformance.repository;

import com.tayperformance.entity.Role;
import com.tayperformance.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository voor User entiteit.
 * Bevat queries voor authenticatie, autorisatie, workload management en analytics.
 */
public interface UserRepository extends JpaRepository<User, Long> {

    // ============================================================
    // AUTHENTICATIE
    // ============================================================

    /**
     * Vind gebruiker op username (= email).
     * Gebruikt voor login.
     */
    Optional<User> findByUsername(String username);

    /**
     * Vind actieve gebruiker op username.
     * Check tijdens login of account actief is.
     */
    Optional<User> findByUsernameAndActiveTrue(String username);

    /**
     * Check of username al bestaat (registratie validatie).
     */
    boolean existsByUsername(String username);

    /**
     * Check of actieve gebruiker met username bestaat.
     */
    boolean existsByUsernameAndActiveTrue(String username);

    // ============================================================
    // BASIS LIJSTEN
    // ============================================================

    /**
     * Alle actieve gebruikers.
     */
    List<User> findAllByActiveTrueOrderByUsernameAsc();

    /**
     * Actieve gebruikers per rol.
     */
    List<User> findAllByRoleAndActiveTrueOrderByUsernameAsc(Role role);

    /**
     * Alle actieve staff members (voor auto-assignment).
     */
    @Query("""
        SELECT u FROM User u
        WHERE u.role = 'STAFF' 
          AND u.active = true
        ORDER BY u.username ASC
    """)
    List<User> findAllActiveStaff();

    /**
     * Alle actieve admins (voor audit notificaties).
     */
    @Query("""
        SELECT u FROM User u
        WHERE u.role = 'ADMIN' 
          AND u.active = true
        ORDER BY u.username ASC
    """)
    List<User> findAllActiveAdmins();

    // ============================================================
    // WORKLOAD MANAGEMENT & AUTO-ASSIGNMENT
    // ============================================================

    /**
     * Staff members gesorteerd op workload (minst druk eerst).
     * Gebruikt voor auto-assignment van nieuwe afspraken.
     *
     * Returns: [User, Long appointmentCount]
     */
    @Query("""
        SELECT u, COUNT(a) as appointmentCount
        FROM User u
        LEFT JOIN Appointment a ON a.assignedStaff.id = u.id
            AND a.startTime >= :startDate
            AND a.startTime < :endDate
            AND a.status IN ('CONFIRMED', 'IN_PROGRESS')
        WHERE u.role = 'STAFF' 
          AND u.active = true
        GROUP BY u
        ORDER BY COUNT(a) ASC, u.username ASC
    """)
    List<Object[]> findStaffByWorkload(
            @Param("startDate") OffsetDateTime startDate,
            @Param("endDate") OffsetDateTime endDate
    );

    /**
     * Helper: Vind minst drukke staff member voor auto-assignment.
     */
    default Optional<User> findLeastBusyStaff(OffsetDateTime startDate, OffsetDateTime endDate) {
        List<Object[]> results = findStaffByWorkload(startDate, endDate);
        return results.isEmpty() ? Optional.empty() : Optional.of((User) results.get(0)[0]);
    }

    /**
     * Staff workload details voor rapportage dashboard.
     */
    @Query("""
        SELECT u.username as username, 
               COUNT(a) as totalAppointments,
               COUNT(CASE WHEN a.status = 'COMPLETED' THEN 1 END) as completedCount,
               COUNT(CASE WHEN a.status IN ('CONFIRMED', 'IN_PROGRESS') THEN 1 END) as activeCount
        FROM User u
        LEFT JOIN Appointment a ON a.assignedStaff.id = u.id
            AND a.startTime >= :startDate
            AND a.startTime < :endDate
        WHERE u.role = 'STAFF' AND u.active = true
        GROUP BY u.id, u.username
        ORDER BY COUNT(a) DESC
    """)
    List<StaffWorkload> getStaffWorkloadReport(
            @Param("startDate") OffsetDateTime startDate,
            @Param("endDate") OffsetDateTime endDate
    );

    // ============================================================
    // ANALYTICS & AUDIT
    // ============================================================

    /**
     * Actieve gebruikers (hebben acties uitgevoerd sinds datum).
     */
    @Query("""
        SELECT DISTINCT a.actor FROM AuditLog a
        WHERE a.actor IS NOT NULL
          AND a.createdAt >= :since
        ORDER BY a.actor.username ASC
    """)
    List<User> findActiveUsersSince(@Param("since") OffsetDateTime since);

    /**
     * Laatste login tijden (via audit log CREATE actie op User).
     */
    @Query("""
        SELECT u.username as username, MAX(a.createdAt) as lastActivityAt
        FROM User u
        LEFT JOIN AuditLog a ON a.actor.id = u.id
        WHERE u.active = true
        GROUP BY u.id, u.username
        ORDER BY MAX(a.createdAt) DESC NULLS LAST
    """)
    List<UserActivity> findUserActivitySummary();

    /**
     * Inactieve gebruikers (geen audit trail sinds datum).
     */
    @Query("""
        SELECT u FROM User u
        WHERE u.active = true
          AND NOT EXISTS (
            SELECT 1 FROM AuditLog a
            WHERE a.actor.id = u.id
              AND a.createdAt >= :since
          )
        ORDER BY u.username ASC
    """)
    List<User> findInactiveSince(@Param("since") OffsetDateTime since);

    // ============================================================
    // RAPPORTAGES
    // ============================================================

    /**
     * Aantal gebruikers per rol.
     * Returns: [Role, Long count]
     */
    @Query("""
        SELECT u.role as role, COUNT(u) as count
        FROM User u
        WHERE u.active = true
        GROUP BY u.role
    """)
    List<RoleCount> countActiveByRole();

    /**
     * Performance metrics per staff member.
     */
    @Query("""
        SELECT u.username as username,
               COUNT(CASE WHEN a.status = 'COMPLETED' THEN 1 END) as completedCount,
               COUNT(CASE WHEN a.status = 'NOSHOW' THEN 1 END) as noShowCount,
               COUNT(CASE WHEN a.status = 'CANCELED' THEN 1 END) as canceledCount,
               AVG(CASE WHEN a.status = 'COMPLETED' AND a.price IS NOT NULL THEN a.price END) as avgRevenue
        FROM User u
        LEFT JOIN Appointment a ON a.assignedStaff.id = u.id
            AND a.startTime >= :startDate
            AND a.startTime < :endDate
        WHERE u.role = 'STAFF' AND u.active = true
        GROUP BY u.id, u.username
        ORDER BY completedCount DESC
    """)
    List<StaffPerformance> getStaffPerformanceReport(
            @Param("startDate") OffsetDateTime startDate,
            @Param("endDate") OffsetDateTime endDate
    );

    // ============================================================
    // STATISTIEKEN
    // ============================================================

    /**
     * Tel actieve gebruikers.
     */
    long countByActiveTrue();

    /**
     * Tel gebruikers per rol.
     */
    long countByRoleAndActiveTrue(Role role);

    /**
     * Nieuwe gebruikers in periode.
     */
    long countByActiveTrueAndCreatedAtBetween(OffsetDateTime start, OffsetDateTime end);
}

// ============================================================
// PROJECTION INTERFACES
// ============================================================

interface RoleCount {
    Role getRole();
    Long getCount();
}

interface StaffWorkload {
    String getUsername();
    Long getTotalAppointments();
    Long getCompletedCount();
    Long getActiveCount();
}

interface UserActivity {
    String getUsername();
    OffsetDateTime getLastActivityAt();
}

interface StaffPerformance {
    String getUsername();
    Long getCompletedCount();
    Long getNoShowCount();
    Long getCanceledCount();
    Double getAvgRevenue();
}