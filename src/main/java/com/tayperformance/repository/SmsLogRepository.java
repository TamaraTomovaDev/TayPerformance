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
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository voor SmsLog entiteit.
 * Bevat queries voor SMS tracking, troubleshooting, analytics en cleanup.
 */
public interface SmsLogRepository extends JpaRepository<SmsLog, Long> {

    // ============================================================
    // BASIS OPVRAGEN PER AFSPRAAK
    // ============================================================

    /**
     * Alle SMS berichten voor een afspraak.
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
     * Helper: Laatste reminder van een afspraak.
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
     * Helper: Laatste confirmation van een afspraak.
     */
    default Optional<SmsLog> findLatestConfirmationByAppointmentId(Long appointmentId) {
        List<SmsLog> confirmations = findConfirmationsByAppointmentId(appointmentId);
        return confirmations.isEmpty() ? Optional.empty() : Optional.of(confirmations.get(0));
    }

    // ============================================================
    // FILTERS & QUEUE MANAGEMENT
    // ============================================================

    /**
     * Gefaalde SMS berichten (voor retry logic).
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
     * SMS berichten in wachtrij ouder dan X minuten (stuck messages).
     */
    @Query("""
        SELECT s FROM SmsLog s
        WHERE s.status = 'QUEUED'
          AND s.createdAt < :before
        ORDER BY s.createdAt ASC
    """)
    List<SmsLog> findStuckInQueue(@Param("before") OffsetDateTime before);

    // ============================================================
    // TELEFOONNUMMER OPZOEKEN
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
     * Helper: Laatste SMS naar een telefoonnummer.
     */
    default Optional<SmsLog> findLatestByToPhone(String toPhone) {
        List<SmsLog> logs = findByToPhoneOrderByCreatedAtDesc(toPhone);
        return logs.isEmpty() ? Optional.empty() : Optional.of(logs.get(0));
    }

    /**
     * Recent verzonden SMS naar nummer (rate limiting check).
     * Bijvoorbeeld: since = now().minusMinutes(5)
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

    // ============================================================
    // ANALYTICS & RAPPORTAGE
    // ============================================================

    /**
     * Status verdeling voor rapportage.
     */
    @Query("""
        SELECT s.status as status, COUNT(s) as count
        FROM SmsLog s
        WHERE s.createdAt >= :since
        GROUP BY s.status
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
    """)
    List<SmsTypeCount> countByTypeSince(@Param("since") OffsetDateTime since);

    /**
     * Success rate berekening (percentage SENT + DELIVERED).
     */
    @Query("""
        SELECT COALESCE(
            COUNT(CASE WHEN s.status IN ('SENT', 'DELIVERED') THEN 1 END) * 100.0 / NULLIF(COUNT(s), 0),
            0.0
        )
        FROM SmsLog s
        WHERE s.createdAt >= :since
    """)
    Double calculateSuccessRateSince(@Param("since") OffsetDateTime since);

    /**
     * Gemiddelde delivery tijd (SENT -> DELIVERED).
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
     * Meest voorkomende foutmeldingen.
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
    // OPSCHONING (RETENTION POLICY)
    // ============================================================

    /**
     * Verlopen SMS logs (voor batch cleanup).
     */
    @Query("""
        SELECT s FROM SmsLog s
        WHERE s.expiresAt IS NOT NULL 
          AND s.expiresAt < :now
        ORDER BY s.expiresAt ASC
    """)
    Page<SmsLog> findExpired(@Param("now") OffsetDateTime now, Pageable pageable);

    /**
     * Veilige batch delete (gebruik in loop tot 0 records).
     * Batch size typisch: 1000
     */
    @Transactional
    @Modifying
    @Query(value = """
        DELETE FROM sms_logs
        WHERE id IN (
            SELECT id FROM sms_logs
            WHERE expires_at IS NOT NULL 
              AND expires_at < :now
            ORDER BY expires_at ASC
            LIMIT :batchSize
        )
        """, nativeQuery = true)
    int deleteExpiredBatch(@Param("now") OffsetDateTime now, @Param("batchSize") int batchSize);

    // ============================================================
    // STATISTIEKEN
    // ============================================================

    /**
     * Tel SMS berichten in periode.
     */
    long countByCreatedAtBetween(OffsetDateTime start, OffsetDateTime end);

    /**
     * Tel gefaalde SMS berichten in periode.
     */
    long countByStatusAndCreatedAtBetween(SmsStatus status, OffsetDateTime start, OffsetDateTime end);

    /**
     * Tel SMS berichten per type.
     */
    long countByTypeAndCreatedAtBetween(SmsType type, OffsetDateTime start, OffsetDateTime end);
}

// ============================================================
// PROJECTION INTERFACES
// ============================================================

interface SmsStatusCount {
    SmsStatus getStatus();
    Long getCount();
}

interface SmsTypeCount {
    SmsType getType();
    Long getCount();
}

interface ErrorMessageCount {
    String getErrorMessage();
    Long getCount();
}