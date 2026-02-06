package com.tayperformance.repository;

import com.tayperformance.entity.AuditLog;
import com.tayperformance.dto.projection.EntityTypeCount;
import com.tayperformance.dto.projection.ActionCount;
import com.tayperformance.dto.projection.UserActionCount;

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
 * Repository voor AuditLog.
 * Biedt audit trails, security monitoring en automatische opschoning.
 */
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    // ============================================================
    // AUDIT TRAILS PER ENTITEIT
    // ============================================================

    /**
     * Volledige geschiedenis van een entiteit.
     */
    List<AuditLog> findAllByEntityTypeAndEntityIdOrderByCreatedAtDesc(
            String entityType, Long entityId
    );

    /**
     * Gepagineerde geschiedenis van een entiteit.
     */
    Page<AuditLog> findAllByEntityTypeAndEntityIdOrderByCreatedAtDesc(
            String entityType, Long entityId, Pageable pageable
    );

    /**
     * Recente wijzigingen op een entiteit.
     */
    @Query("""
        SELECT a FROM AuditLog a
        WHERE a.entityType = :type
          AND a.entityId = :id
          AND a.createdAt >= :since
        ORDER BY a.createdAt DESC
    """)
    List<AuditLog> findRecentForEntity(
            @Param("type") String entityType,
            @Param("id") Long entityId,
            @Param("since") OffsetDateTime since
    );

    // ============================================================
    // AUDIT TRAILS PER GEBRUIKER
    // ============================================================

    /**
     * Alle acties door een gebruiker.
     */
    List<AuditLog> findAllByActorIdOrderByCreatedAtDesc(Long actorId);

    /**
     * Recente acties door gebruiker (bijv. laatste 24u).
     */
    @Query("""
        SELECT a FROM AuditLog a
        WHERE a.actor.id = :actorId
          AND a.createdAt >= :since
        ORDER BY a.createdAt DESC
    """)
    List<AuditLog> findUserActionsSince(
            @Param("actorId") Long actorId,
            @Param("since") OffsetDateTime since
    );

    // ============================================================
    // FILTERS PER ENTITY TYPE
    // ============================================================

    /**
     * Alle logs voor een entity type (bijv Customer, Appointment).
     */
    Page<AuditLog> findAllByEntityTypeOrderByCreatedAtDesc(
            String entityType, Pageable pageable
    );

    /**
     * Logs in een periode voor een entity type.
     */
    @Query("""
        SELECT a FROM AuditLog a
        WHERE a.entityType = :entityType
          AND a.createdAt >= :start
          AND a.createdAt < :end
        ORDER BY a.createdAt DESC
    """)
    Page<AuditLog> findByEntityTypeBetweenDates(
            @Param("entityType") String entityType,
            @Param("start") OffsetDateTime start,
            @Param("end") OffsetDateTime end,
            Pageable pageable
    );

    // ============================================================
    // FILTERS PER ACTIE
    // ============================================================

    Page<AuditLog> findAllByActionOrderByCreatedAtDesc(String action, Pageable pageable);

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
    // SECURITY / SYSTEM MONITORING
    // ============================================================

    /**
     * Recente activiteit (handig voor dashboard).
     */
    @Query("""
        SELECT a FROM AuditLog a
        WHERE a.createdAt >= :since
        ORDER BY a.createdAt DESC
    """)
    Page<AuditLog> findRecentActivity(
            @Param("since") OffsetDateTime since,
            Pageable pageable
    );

    /**
     * System actions (actor is null).
     */
    @Query("""
        SELECT a FROM AuditLog a
        WHERE a.actor IS NULL
          AND a.createdAt >= :since
        ORDER BY a.createdAt DESC
    """)
    List<AuditLog> findSystemActionsSince(@Param("since") OffsetDateTime since);

    // ============================================================
    // OPSCHONING (RETENTION POLICY 90 DAGEN)
    // ============================================================

    /**
     * Verlopen logs ophalen voor batchverwerking.
     */
    @Query("""
        SELECT a FROM AuditLog a
        WHERE a.expiresAt IS NOT NULL
          AND a.expiresAt < :now
        ORDER BY a.expiresAt ASC
    """)
    Page<AuditLog> findExpired(
            @Param("now") OffsetDateTime now,
            Pageable pageable
    );

    /**
     * Batch delete (limiet instelbaar).
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
            LIMIT :limit
        )
        """, nativeQuery = true)
    int deleteExpiredBatch(
            @Param("now") OffsetDateTime now,
            @Param("limit") int limit
    );

    // ============================================================
    // STATISTIEKEN / PROJECTIONS
    // ============================================================

    @Query("""
        SELECT a.entityType AS entityType, COUNT(a) AS count
        FROM AuditLog a
        WHERE a.createdAt >= :since
        GROUP BY a.entityType
        ORDER BY COUNT(a) DESC
    """)
    List<EntityTypeCount> countEntityTypesSince(@Param("since") OffsetDateTime since);

    @Query("""
        SELECT a.action AS action, COUNT(a) AS count
        FROM AuditLog a
        WHERE a.createdAt >= :since
        GROUP BY a.action
        ORDER BY COUNT(a) DESC
    """)
    List<ActionCount> countActionsSince(@Param("since") OffsetDateTime since);

    @Query("""
        SELECT a.actor.username AS username, COUNT(a) AS actionCount
        FROM AuditLog a
        WHERE a.actor IS NOT NULL
          AND a.createdAt >= :since
        GROUP BY a.actor.id, a.actor.username
        ORDER BY COUNT(a) DESC
    """)
    List<UserActionCount> countUserActionsSince(@Param("since") OffsetDateTime since);
}