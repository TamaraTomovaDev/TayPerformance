package com.tayperformance.repository;

import com.tayperformance.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Repository voor AuditLog entiteit.
 * Bevat queries voor audit trail, compliance reporting en cleanup.
 */
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    // ============================================================
    // OPHALEN PER ENTITEIT
    // ============================================================

    /**
     * Volledige audit trail voor een specifieke entiteit.
     * Bijvoorbeeld: entityType="Appointment", entityId=123
     */
    List<AuditLog> findAllByEntityTypeAndEntityIdOrderByCreatedAtDesc(
            String entityType,
            Long entityId
    );

    /**
     * Gepagineerde audit trail voor een entiteit.
     */
    Page<AuditLog> findAllByEntityTypeAndEntityIdOrderByCreatedAtDesc(
            String entityType,
            Long entityId,
            Pageable pageable
    );

    /**
     * Recente wijzigingen op een entiteit.
     */
    @Query("""
        SELECT a FROM AuditLog a
        WHERE a.entityType = :entityType
          AND a.entityId = :entityId
          AND a.createdAt >= :since
        ORDER BY a.createdAt DESC
    """)
    List<AuditLog> findRecentByEntity(
            @Param("entityType") String entityType,
            @Param("entityId") Long entityId,
            @Param("since") OffsetDateTime since
    );

    // ============================================================
    // OPHALEN PER ACTOR (WIE DEED WAT?)
    // ============================================================

    /**
     * Alle acties van een gebruiker.
     */
    List<AuditLog> findAllByActorIdOrderByCreatedAtDesc(Long actorId);

    /**
     * Recente acties van een gebruiker.
     */
    @Query("""
        SELECT a FROM AuditLog a
        WHERE a.actor.id = :actorId 
          AND a.createdAt >= :since
        ORDER BY a.createdAt DESC
    """)
    List<AuditLog> findByActorSince(
            @Param("actorId") Long actorId,
            @Param("since") OffsetDateTime since
    );

    /**
     * Gepagineerde acties van een gebruiker.
     */
    Page<AuditLog> findAllByActorIdOrderByCreatedAtDesc(Long actorId, Pageable pageable);

    // ============================================================
    // OPHALEN PER ENTITY TYPE
    // ============================================================

    /**
     * Alle logs voor een entity type (bijv. alle Appointment wijzigingen).
     */
    Page<AuditLog> findAllByEntityTypeOrderByCreatedAtDesc(String entityType, Pageable pageable);

    /**
     * Logs voor entity type in periode.
     */
    @Query("""
        SELECT a FROM AuditLog a
        WHERE a.entityType = :entityType
          AND a.createdAt >= :startDate
          AND a.createdAt < :endDate
        ORDER BY a.createdAt DESC
    """)
    List<AuditLog> findByEntityTypeBetweenDates(
            @Param("entityType") String entityType,
            @Param("startDate") OffsetDateTime startDate,
            @Param("endDate") OffsetDateTime endDate
    );

    /**
     * Logs voor entity type in periode (gepagineerd).
     */
    @Query("""
        SELECT a FROM AuditLog a
        WHERE a.entityType = :entityType
          AND a.createdAt >= :startDate
          AND a.createdAt < :endDate
        ORDER BY a.createdAt DESC
    """)
    Page<AuditLog> findByEntityTypeBetweenDates(
            @Param("entityType") String entityType,
            @Param("startDate") OffsetDateTime startDate,
            @Param("endDate") OffsetDateTime endDate,
            Pageable pageable
    );

    // ============================================================
    // OPHALEN PER ACTIE TYPE
    // ============================================================

    /**
     * Alle logs voor een specifieke actie (CREATE, UPDATE, DELETE, etc.).
     */
    List<AuditLog> findAllByActionOrderByCreatedAtDesc(String action);

    /**
     * Gepagineerde logs per actie.
     */
    Page<AuditLog> findAllByActionOrderByCreatedAtDesc(String action, Pageable pageable);

    /**
     * Specifieke actie op entity type.
     * Bijvoorbeeld: action="DELETE", entityType="Customer"
     */
    @Query("""
        SELECT a FROM AuditLog a
        WHERE a.action = :action
          AND a.entityType = :entityType
        ORDER BY a.createdAt DESC
    """)
    List<AuditLog> findByActionAndEntityType(
            @Param("action") String action,
            @Param("entityType") String entityType
    );

    // ============================================================
    // COMPLIANCE & SECURITY QUERIES
    // ============================================================

    /**
     * Recente activiteit voor security monitoring.
     * Typisch: since = now().minusHours(24)
     */
    @Query("""
        SELECT a FROM AuditLog a
        WHERE a.createdAt >= :since
        ORDER BY a.createdAt DESC
    """)
    Page<AuditLog> findRecentActivity(@Param("since") OffsetDateTime since, Pageable pageable);

    /**
     * Verdachte acties (bijv. veel deletes).
     */
    @Query("""
        SELECT a FROM AuditLog a
        WHERE a.action IN :suspiciousActions
          AND a.createdAt >= :since
        ORDER BY a.createdAt DESC
    """)
    List<AuditLog> findSuspiciousActions(
            @Param("suspiciousActions") List<String> suspiciousActions,
            @Param("since") OffsetDateTime since
    );

    /**
     * Wijzigingen door systeem (actor = null).
     */
    @Query("""
        SELECT a FROM AuditLog a
        WHERE a.actor IS NULL
          AND a.createdAt >= :since
        ORDER BY a.createdAt DESC
    """)
    List<AuditLog> findSystemActions(@Param("since") OffsetDateTime since);

    /**
     * Bulk acties (zelfde actor, veel acties in korte tijd).
     */
    @Query("""
        SELECT a.actor.username as username, 
               COUNT(a) as actionCount,
               MIN(a.createdAt) as firstAction,
               MAX(a.createdAt) as lastAction
        FROM AuditLog a
        WHERE a.actor IS NOT NULL
          AND a.createdAt >= :since
        GROUP BY a.actor.id, a.actor.username
        HAVING COUNT(a) > :threshold
        ORDER BY COUNT(a) DESC
    """)
    List<BulkActionSummary> findBulkActions(
            @Param("since") OffsetDateTime since,
            @Param("threshold") long threshold
    );

    // ============================================================
    // STATISTIEKEN & RAPPORTAGE
    // ============================================================

    /**
     * Aantal logs per entity type.
     */
    @Query("""
        SELECT a.entityType as entityType, COUNT(a) as count
        FROM AuditLog a
        WHERE a.createdAt >= :since
        GROUP BY a.entityType
        ORDER BY COUNT(a) DESC
    """)
    List<EntityTypeCount> countByEntityTypeSince(@Param("since") OffsetDateTime since);

    /**
     * Aantal logs per actie type.
     */
    @Query("""
        SELECT a.action as action, COUNT(a) as count
        FROM AuditLog a
        WHERE a.createdAt >= :since
        GROUP BY a.action
        ORDER BY COUNT(a) DESC
    """)
    List<ActionCount> countByActionSince(@Param("since") OffsetDateTime since);

    /**
     * Activiteit per gebruiker.
     */
    @Query("""
        SELECT a.actor.username as username, COUNT(a) as actionCount
        FROM AuditLog a
        WHERE a.actor IS NOT NULL
          AND a.createdAt >= :since
        GROUP BY a.actor.id, a.actor.username
        ORDER BY COUNT(a) DESC
    """)
    List<UserActionCount> countByActorSince(@Param("since") OffsetDateTime since);

    /**
     * Dagelijkse activiteit trend.
     */
    @Query("""
        SELECT DATE(a.createdAt) as date, COUNT(a) as count
        FROM AuditLog a
        WHERE a.createdAt >= :since
        GROUP BY DATE(a.createdAt)
        ORDER BY DATE(a.createdAt) ASC
    """)
    List<DailyActivity> countDailyActivity(@Param("since") OffsetDateTime since);

    // ============================================================
    // OPSCHONING (RETENTION POLICY)
    // ============================================================

    /**
     * Verlopen audit logs (voor batch cleanup).
     */
    @Query("""
        SELECT a FROM AuditLog a
        WHERE a.expiresAt IS NOT NULL 
          AND a.expiresAt < :now
        ORDER BY a.expiresAt ASC
    """)
    Page<AuditLog> findExpired(@Param("now") OffsetDateTime now, Pageable pageable);

    /**
     * Veilige batch delete (gebruik in loop tot 0 records).
     * Batch size typisch: 1000
     */
    @Transactional
    @Modifying
    @Query(value = """
        DELETE FROM audit_logs
        WHERE id IN (
            SELECT id FROM audit_logs
            WHERE expires_at IS NOT NULL 
              AND expires_at < :now
            ORDER BY expires_at ASC
            LIMIT :batchSize
        )
        """, nativeQuery = true)
    int deleteExpiredBatch(@Param("now") OffsetDateTime now, @Param("batchSize") int batchSize);

    // ============================================================
    // ALGEMENE STATISTIEKEN
    // ============================================================

    /**
     * Tel logs in periode.
     */
    long countByCreatedAtBetween(OffsetDateTime start, OffsetDateTime end);

    /**
     * Tel logs per entity type.
     */
    long countByEntityType(String entityType);

    /**
     * Tel logs per actie.
     */
    long countByAction(String action);
}

// ============================================================
// PROJECTION INTERFACES
// ============================================================

interface EntityTypeCount {
    String getEntityType();
    Long getCount();
}

interface ActionCount {
    String getAction();
    Long getCount();
}

interface UserActionCount {
    String getUsername();
    Long getActionCount();
}

interface DailyActivity {
    String getDate();
    Long getCount();
}

interface BulkActionSummary {
    String getUsername();
    Long getActionCount();
    OffsetDateTime getFirstAction();
    OffsetDateTime getLastAction();
}