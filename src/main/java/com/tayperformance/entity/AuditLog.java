package com.tayperformance.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;

/**
 * Audit Log entity - Volledige audit trail van alle wijzigingen in het systeem.
 *
 * Registreert:
 * - WIE heeft de actie uitgevoerd (User)
 * - WAT is er gebeurd (action: CREATE, UPDATE, DELETE)
 * - WANNEER is het gebeurd (createdAt)
 * - OP WELKE entiteit (entityType + entityId)
 * - WELKE details (detailsJson: voor/na waardes)
 *
 * Gebruikt voor:
 * - Security auditing
 * - Troubleshooting
 * - Compliance (GDPR Article 30: logging)
 * - Dispute resolution
 *
 * RETENTION POLICY: 90 dagen (automatisch opgeschoond)
 */
@Entity
@Table(
        name = "audit_logs",
        indexes = {
                @Index(name = "idx_audit_entity", columnList = "entity_type,entity_id"),
                @Index(name = "idx_audit_created", columnList = "created_at"),
                @Index(name = "idx_audit_user", columnList = "actor_user_id"),
                @Index(name = "idx_audit_expires", columnList = "expires_at")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"actor"})
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class AuditLog {

    @EqualsAndHashCode.Include
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ============================================================
    // ENTITEIT INFORMATIE (WAT is er gewijzigd?)
    // ============================================================

    /**
     * Type van de entiteit die gewijzigd is.
     *
     * Voorbeelden:
     * - "Appointment"
     * - "Customer"
     * - "User"
     * - "DetailService"
     *
     * Gebruikt samen met entityId om exact te weten welk record.
     */
    @NotBlank(message = "Entity type is verplicht")
    @Size(max = 60, message = "Entity type mag maximaal 60 karakters zijn")
    @Column(name = "entity_type", nullable = false, length = 60)
    private String entityType;

    /**
     * ID van het gewijzigde record.
     * Bijvoorbeeld: Appointment met id=123
     */
    @NotNull(message = "Entity ID is verplicht")
    @Column(name = "entity_id", nullable = false)
    private Long entityId;

    // ============================================================
    // ACTIE INFORMATIE (WAT is er gedaan?)
    // ============================================================

    /**
     * Actie die uitgevoerd is.
     *
     * Standaard acties:
     * - "CREATE"           → Nieuw record aangemaakt
     * - "UPDATE"           → Bestaand record gewijzigd
     * - "DELETE"           → Record verwijderd (of soft delete)
     * - "STATUS_CHANGE"    → Specifiek voor status wijzigingen
     * - "CONFIRM"          → Afspraak bevestigd
     * - "CANCEL"           → Afspraak geannuleerd
     * - "RESCHEDULE"       → Afspraak verplaatst
     *
     * Domein-specifieke acties toegestaan voor duidelijkheid.
     */
    @NotBlank(message = "Actie is verplicht")
    @Size(max = 60, message = "Actie mag maximaal 60 karakters zijn")
    @Column(nullable = false, length = 60)
    private String action;

    /**
     * Gedetailleerde informatie over de wijziging in JSON formaat.
     *
     * Voorbeelden:
     *
     * CREATE:
     * {
     *   "appointmentId": 123,
     *   "customerId": 45,
     *   "startTime": "2026-02-15T14:00:00+01:00",
     *   "status": "REQUESTED"
     * }
     *
     * UPDATE:
     * {
     *   "field": "startTime",
     *   "oldValue": "2026-02-15T14:00:00+01:00",
     *   "newValue": "2026-02-16T10:00:00+01:00"
     * }
     *
     * STATUS_CHANGE:
     * {
     *   "oldStatus": "REQUESTED",
     *   "newStatus": "CONFIRMED",
     *   "reason": "Customer called to confirm"
     * }
     *
     * ⚠️ PRIVACY: Sla GEEN gevoelige data op (wachtwoorden, volledige betaaldata).
     */
    @Size(max = 10000, message = "Details JSON mag maximaal 10000 karakters zijn")
    @Column(name = "details_json", columnDefinition = "TEXT")
    private String detailsJson;

    // ============================================================
    // ACTOR (WIE heeft het gedaan?)
    // ============================================================

    /**
     * Gebruiker die de actie heeft uitgevoerd.
     *
     * Null indien:
     * - Automatische actie (bijv. batch job voor reminders)
     * - Systeem actie (bijv. auto-archivering)
     * - Klant actie via publieke website (geen account)
     *
     * Voor klant acties: vermeld in detailsJson bijv:
     * {"source": "public_website", "customerPhone": "+33612345678"}
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_user_id")
    @JsonIgnore
    private User actor;

    // ============================================================
    // TIMESTAMPS
    // ============================================================

    /**
     * Tijdstip waarop de actie plaatsvond.
     * Automatisch gezet door Hibernate.
     */
    @NotNull
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    /**
     * Vervaldatum voor automatische opschoning.
     * Default: 90 dagen na aanmaak.
     *
     * Waarom 90 dagen?
     * - Voldoende voor recente troubleshooting
     * - Compliance met data minimalisatie (GDPR)
     * - Bespaart database opslag
     *
     * ADMIN kan langere retentie instellen voor specifieke logs indien nodig.
     */
    @Column(name = "expires_at")
    private OffsetDateTime expiresAt;

    // ============================================================
    // LIFECYCLE CALLBACKS
    // ============================================================

    /**
     * Zet automatisch de vervaldatum voor het opslaan.
     * Audit logs worden na 90 dagen automatisch verwijderd (batch job).
     */
    @PrePersist
    private void setExpiryDate() {
        if (expiresAt == null && createdAt != null) {
            this.expiresAt = createdAt.plusDays(90);
        } else if (expiresAt == null) {
            // Fallback als createdAt nog niet gezet is
            this.expiresAt = OffsetDateTime.now().plusDays(90);
        }
    }

    // ============================================================
    // HELPER METHODS
    // ============================================================

    /**
     * Geeft een menselijk leesbare beschrijving van de log entry.
     * Bijvoorbeeld: "User admin@example.com UPDATED Appointment #123"
     */
    public String getDescription() {
        String actorName = (actor != null) ? actor.getUsername() : "SYSTEM";
        return String.format("%s %s %s #%d",
                actorName,
                action,
                entityType,
                entityId
        );
    }

    /**
     * Check of deze log verlopen is en verwijderd kan worden.
     */
    public boolean isExpired() {
        return expiresAt != null && expiresAt.isBefore(OffsetDateTime.now());
    }
}