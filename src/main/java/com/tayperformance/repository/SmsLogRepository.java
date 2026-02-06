package com.tayperformance.repository;

import com.tayperformance.entity.SmsLog;
import com.tayperformance.entity.SmsStatus;
import com.tayperformance.entity.SmsType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository voor SmsLog entiteit.
 *
 * Features:
 * - SMS tracking per afspraak
 * - Queue management (QUEUED, stuck messages)
 * - Analytics & rapportage
 * - Retention policy cleanup
 * - Rate limiting checks
 */
public interface SmsLogRepository extends JpaRepository<SmsLog, Long> {

    // ============================================================
    // BASIS QUERIES - PER AFSPRAAK
    // ============================================================

    /**
     * Alle SMS berichten voor een afspraak (meest recent eerst).
     */
    List<SmsLog> findAllByAppointmentIdOrderByCreatedAtDesc(Long appointmentId);

    /**
     * Alle reminders voor een afspraak.
     */
    @Query("""
        SELECT s FROM SmsLog s
        WHERE s.appointment.id = :appointmentId
          AND s.type = 'REMINDER'
        ORDER BY s.createdAt DESC
    """)
    List<SmsLog> findRemindersByAppointmentId(@Param("appointmentId") Long appointmentId);

    /**
     * Laatste reminder van een afspraak.
     */
    default Optional<SmsLog> findLatestReminderByAppointmentId(Long appointmentId) {
        List<SmsLog> reminders = findRemindersByAppointmentId(appointmentId);
        return reminders.isEmpty() ? Optional.empty() : Optional.of(reminders.get(0));
    }

    /**
     * Check of reminder al verstuurd is (voorkomt duplicaten).
     */
    @Query("""
        SELECT CASE WHEN COUNT(s) > 0 THEN true ELSE false END
        FROM SmsLog s
        WHERE s.appointment.id = :appointmentId
          AND s.type = 'REMINDER'
          AND s.status IN ('SENT', 'DELIVERED')
    """)
    boolean hasReminderBeenSent(@Param("appointmentId") Long appointmentId);

    /**
     * Alle confirmations voor een afspraak.
     */
    @Query("""
        SELECT s FROM SmsLog s
        WHERE s.appointment.id = :appointmentId
          AND s.type = 'CONFIRM'
        ORDER BY s.createdAt DESC
    """)
    List<SmsLog> findConfirmationsByAppointmentId(@Param("appointmentId") Long appointmentId);

    /**
     * Laatste confirmation van een afspraak.
     */
    default Optional<SmsLog> findLatestConfirmationByAppointmentId(Long appointmentId) {
        List<SmsLog> confirmations = findConfirmationsByAppointmentId(appointmentId);
        return confirmations.isEmpty() ? Optional.empty() : Optional.of(confirmations.get(0));
    }

    // ============================================================
    // QUEUE MANAGEMENT
    // ============================================================

    /**
     * Gefaalde SMS berichten (voor retry logic).
     * Gebruik pageable voor batch processing.
     */
    @Query("""
        SELECT s FROM SmsLog s
        WHERE s.status = 'FAILED' 
          AND s.createdAt >= :since
        ORDER BY s.createdAt ASC
    """)
    Page<SmsLog> findFailedSince(@Param("since") OffsetDateTime since, Pageable pageable);

    /**
     * SMS berichten in wachtrij (voor batch verzending).
     */
    @Query("""
        SELECT s FROM SmsLog s
        WHERE s.status = 'QUEUED'
        ORDER BY s.createdAt ASC
    """)
    List<SmsLog> findQueued();

    /**
     * Stuck messages: in queue maar ouder dan X minuten.
     * Gebruik voor monitoring/alerting.
     */
    @Query("""
        SELECT s FROM SmsLog s
        WHERE s.status = 'QUEUED'
          AND s.createdAt < :before
        ORDER BY s.createdAt ASC
    """)
    List<SmsLog> findStuckInQueue(@Param("before") OffsetDateTime before);

    // ============================================================
    // TELEFOONNUMMER QUERIES
    // ============================================================

    /**
     * Alle SMS berichten naar een telefoonnummer.
     */
    @Query("""
        SELECT s FROM SmsLog s
        WHERE s.toPhone = :toPhone
        ORDER BY s.createdAt DESC
    """)
    List<SmsLog> findByToPhoneOrderByCreatedAtDesc(@Param("toPhone") String toPhone);

    /**
     * Laatste SMS naar een telefoonnummer.
     */
    default Optional<SmsLog> findLatestByToPhone(String toPhone) {
        List<SmsLog> logs = findByToPhoneOrderByCreatedAtDesc(toPhone);
        return logs.isEmpty() ? Optional.empty() : Optional.of(logs.get(0));
    }

    /**
     * Recent verzonden SMS naar nummer (rate limiting check).
     * Voorbeeld: since = now().minusMinutes(5) voor 5-min rate limit
     */
    @Query("""
        SELECT s FROM SmsLog s
        WHERE s.toPhone = :toPhone
          AND s.createdAt >= :since
        ORDER BY s.createdAt DESC
    """)
    List<SmsLog> findRecentByToPhone(
            @Param("toPhone") String toPhone,
            @Param("since") OffsetDateTime since
    );

    /**
     * Tel recente SMS naar nummer (rate limit counter).
     */
    @Query("""
        SELECT COUNT(s)
        FROM SmsLog s
        WHERE s.toPhone = :toPhone
          AND s.createdAt >= :since
    """)
    long countRecentByToPhone(
            @Param("toPhone") String toPhone,
            @Param("since") OffsetDateTime since
    );

    // ============================================================
    // ANALYTICS & RAPPORTAGE
    // ============================================================

    /**
     * Status verdeling voor dashboard.
     */
    @Query("""
        SELECT s.status as status, COUNT(s) as count
        FROM SmsLog s
        WHERE s.createdAt >= :since
        GROUP BY s.status
        ORDER BY COUNT(s) DESC
    """)
    List<SmsStatusCount> countByStatusSince(@Param("since") OffsetDateTime since);

    /**
     * Type verdeling voor rapportage.
     */
    @Query("""
        SELECT s.type as type, COUNT(s) as count
        FROM SmsLog s
        WHERE s.createdAt >= :since
        GROUP BY s.type
        ORDER BY COUNT(s) DESC
    """)
    List<SmsTypeCount> countByTypeSince(@Param("since") OffsetDateTime since);

    /**
     * Success rate berekening (percentage SENT + DELIVERED).
     * Retourneert 0.0 bij geen berichten.
     */
    @Query("""
        SELECT COALESCE(
            COUNT(CASE WHEN s.status IN ('SENT', 'DELIVERED') THEN 1 END) * 100.0 / 
            NULLIF(COUNT(s), 0),
            0.0
        )
        FROM SmsLog s
        WHERE s.createdAt >= :since
    """)
    Double calculateSuccessRateSince(@Param("since") OffsetDateTime since);

    /**
     * Gemiddelde delivery tijd in seconden (SENT -> DELIVERED).
     * Retourneert null indien geen delivered berichten.
     */
    @Query("""
        SELECT AVG(EXTRACT(EPOCH FROM (s.deliveredAt - s.sentAt)))
        FROM SmsLog s
        WHERE s.status = 'DELIVERED'
          AND s.sentAt IS NOT NULL
          AND s.deliveredAt IS NOT NULL
          AND s.createdAt >= :since
    """)
    Double calculateAverageDeliveryTimeSeconds(@Param("since") OffsetDateTime since);

    /**
     * Meest voorkomende foutmeldingen (top errors).
     */
    @Query("""
        SELECT s.errorMessage as errorMessage, COUNT(s) as count
        FROM SmsLog s
        WHERE s.status = 'FAILED'
          AND s.errorMessage IS NOT NULL
          AND s.createdAt >= :since
        GROUP BY s.errorMessage
        ORDER BY COUNT(s) DESC
    """)
    List<ErrorMessageCount> findMostCommonErrors(@Param("since") OffsetDateTime since);

    // ============================================================
    // RETENTION POLICY & CLEANUP
    // ============================================================

    /**
     * Verlopen SMS logs (voor cleanup job).
     * Gebruik pageable voor batch processing.
     */
    @Query("""
        SELECT s FROM SmsLog s
        WHERE s.expiresAt IS NOT NULL 
          AND s.expiresAt < :now
        ORDER BY s.expiresAt ASC
    """)
    Page<SmsLog> findExpired(@Param("now") OffsetDateTime now, Pageable pageable);

    /**
     * Batch delete van verlopen logs.
     *
     * BELANGRIJK:
     * - Gebruik in loop tot 0 records verwijderd
     * - Typische batchSize: 1000
     * - Run tijdens off-peak hours
     *
     * Voorbeeld usage:
     * <pre>
     * int deleted;
     * do {
     *     deleted = repository.deleteExpiredBatch(now, 1000);
     *     log.info("Deleted {} expired SMS logs", deleted);
     * } while (deleted > 0);
     * </pre>
     */
    @Modifying
    @Query(value = """
        DELETE FROM SmsLog s
        WHERE s.id IN (
            SELECT sl.id FROM SmsLog sl
            WHERE sl.expiresAt IS NOT NULL 
              AND sl.expiresAt < :now
            ORDER BY sl.expiresAt ASC
            LIMIT :batchSize
        )
        """)
    int deleteExpiredBatch(@Param("now") OffsetDateTime now, @Param("batchSize") int batchSize);

    /**
     * Tel verlopen logs (voor monitoring).
     */
    @Query("""
        SELECT COUNT(s)
        FROM SmsLog s
        WHERE s.expiresAt IS NOT NULL 
          AND s.expiresAt < :now
    """)
    long countExpired(@Param("now") OffsetDateTime now);

    // ============================================================
    // STATISTIEKEN & COUNTING
    // ============================================================

    /**
     * Tel SMS berichten in periode.
     */
    long countByCreatedAtBetween(OffsetDateTime start, OffsetDateTime end);

    /**
     * Tel berichten per status in periode.
     */
    long countByStatusAndCreatedAtBetween(
            SmsStatus status,
            OffsetDateTime start,
            OffsetDateTime end
    );

    /**
     * Tel berichten per type in periode.
     */
    long countByTypeAndCreatedAtBetween(
            SmsType type,
            OffsetDateTime start,
            OffsetDateTime end
    );

    /**
     * Tel berichten per status (all time).
     */
    long countByStatus(SmsStatus status);

    /**
     * Tel berichten per type (all time).
     */
    long countByType(SmsType type);

    // ============================================================
    // TROUBLESHOOTING QUERIES
    // ============================================================

    /**
     * Vind berichten met specifieke provider message ID.
     * Nuttig voor troubleshooting met Twilio/etc.
     */
    Optional<SmsLog> findByProviderMessageId(String providerMessageId);

    /**
     * Vind alle berichten voor een afspraak met specifieke status.
     */
    @Query("""
        SELECT s FROM SmsLog s
        WHERE s.appointment.id = :appointmentId
          AND s.status = :status
        ORDER BY s.createdAt DESC
    """)
    List<SmsLog> findByAppointmentIdAndStatus(
            @Param("appointmentId") Long appointmentId,
            @Param("status") SmsStatus status
    );

    /**
     * Vind alle gefaalde berichten voor een telefoonnummer.
     * Nuttig voor blacklist/blokkering.
     */
    @Query("""
        SELECT s FROM SmsLog s
        WHERE s.toPhone = :toPhone
          AND s.status = 'FAILED'
          AND s.createdAt >= :since
        ORDER BY s.createdAt DESC
    """)
    List<SmsLog> findFailedByToPhoneSince(
            @Param("toPhone") String toPhone,
            @Param("since") OffsetDateTime since
    );
}

// ============================================================
// PROJECTION INTERFACES (voor analytics queries)
// ============================================================

/**
 * Status count projection voor dashboard.
 */
interface SmsStatusCount {
    SmsStatus getStatus();
    Long getCount();
}

/**
 * Type count projection voor rapportage.
 */
interface SmsTypeCount {
    SmsType getType();
    Long getCount();
}

/**
 * Error message count projection voor troubleshooting.
 */
interface ErrorMessageCount {
    String getErrorMessage();
    Long getCount();
}