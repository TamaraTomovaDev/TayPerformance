package com.tayperformance.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;

/**
 * SMS Log entity - Volledige audit trail van alle SMS communicatie.
 *
 * Gebruikt voor:
 * - Troubleshooting (waarom heeft klant geen SMS ontvangen?)
 * - Legal compliance (bewijs van communicatie)
 * - Cost tracking (aantal verzonden SMS'en)
 * - Retry logic (gefaalde SMS'en opnieuw versturen)
 *
 * RETENTION POLICY: Automatisch verwijderen na 30 dagen om opslag te besparen.
 */
@Entity
@Table(
        name = "sms_logs",
        indexes = {
                @Index(name = "idx_sms_appt", columnList = "appointment_id"),
                @Index(name = "idx_sms_status", columnList = "status"),
                @Index(name = "idx_sms_created", columnList = "created_at"),
                @Index(name = "idx_sms_expires", columnList = "expires_at")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"appointment"})
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class SmsLog {

    @EqualsAndHashCode.Include
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Versie voor optimistic locking.
     * Belangrijk bij retry logic.
     */
    @Version
    private Long version;

    // ============================================================
    // RELATIES
    // ============================================================

    /**
     * Afspraak waarvoor deze SMS verstuurd werd.
     * Elke SMS is gekoppeld aan een afspraak.
     */
    @NotNull(message = "Afspraak is verplicht")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "appointment_id", nullable = false)
    @JsonIgnore
    private Appointment appointment;

    // ============================================================
    // SMS INFORMATIE
    // ============================================================

    /**
     * Type SMS bericht.
     * Zie SmsType enum: CONFIRM, UPDATE, CANCEL, REMINDER
     */
    @NotNull(message = "SMS type is verplicht")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SmsType type;

    /**
     * Status van de SMS.
     * Zie SmsStatus enum: QUEUED, SENT, DELIVERED, FAILED
     *
     * Workflow:
     * 1. QUEUED    → SMS wacht in verzendwachtrij
     * 2. SENT      → SMS verstuurd naar provider (Twilio/Vonage/etc)
     * 3. DELIVERED → SMS aangekomen bij klant (webhook van provider)
     * 4. FAILED    → SMS niet verstuurd (fout, ongeldig nummer, etc)
     */
    @NotNull(message = "SMS status is verplicht")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private SmsStatus status = SmsStatus.QUEUED;

    /**
     * Telefoonnummer van de ontvanger.
     * Gekopieerd van Customer.phone voor audit trail.
     * E.164 formaat (bijvoorbeeld: +33612345678)
     */
    @NotBlank(message = "Telefoonnummer is verplicht")
    @Pattern(
            regexp = "^\\+?[1-9]\\d{1,14}$",
            message = "Ongeldig telefoonnummer formaat (E.164)"
    )
    @Column(name = "to_phone", nullable = false, length = 30)
    private String toPhone;

    /**
     * Inhoud van het SMS bericht.
     *
     * Bijvoorbeeld:
     * "Bonjour! Votre RDV Tay Performance est confirmé le 15/02/2026 à 14h00.
     *  Adresse: 123 Rue Example, Strasbourg. À bientôt!"
     *
     * Max 1600 tekens (10 SMS segmenten).
     */
    @NotBlank(message = "SMS inhoud is verplicht")
    @Size(min = 10, max = 1600, message = "SMS moet tussen 10 en 1600 karakters zijn")
    @Column(name = "message_body", columnDefinition = "TEXT", nullable = false)
    private String messageBody;

    // ============================================================
    // PROVIDER INFORMATIE
    // ============================================================

    /**
     * Unieke ID van de SMS bij de provider (Twilio, Vonage, etc).
     * Gebruikt voor:
     * - Status tracking via webhooks
     * - Troubleshooting met provider support
     * - Cost reconciliation
     *
     * Null als SMS nog niet verstuurd (status = QUEUED).
     */
    @Size(max = 120, message = "Provider message ID mag maximaal 120 karakters zijn")
    @Column(name = "provider_message_id", length = 120)
    private String providerMessageId;

    /**
     * Foutmelding indien SMS gefaald is.
     *
     * Voorbeelden:
     * - "Invalid phone number"
     * - "Account out of credit"
     * - "Carrier rejected message"
     *
     * Null als status != FAILED.
     */
    @Size(max = 1000, message = "Foutmelding mag maximaal 1000 karakters zijn")
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    // ============================================================
    // TIMESTAMPS
    // ============================================================

    /**
     * Tijdstip waarop SMS record aangemaakt werd (in wachtrij gezet).
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    /**
     * Tijdstip waarop SMS verstuurd werd naar provider.
     * Null als status = QUEUED.
     */
    @Column(name = "sent_at")
    private OffsetDateTime sentAt;

    /**
     * Tijdstip waarop SMS aangekomen is bij klant (webhook van provider).
     * Null als status != DELIVERED.
     */
    @Column(name = "delivered_at")
    private OffsetDateTime deliveredAt;

    /**
     * Vervaldatum voor automatische opschoning.
     * Default: 30 dagen na aanmaak.
     * Zie @PrePersist lifecycle callback.
     */
    @Column(name = "expires_at")
    private OffsetDateTime expiresAt;

    // ============================================================
    // LIFECYCLE CALLBACKS
    // ============================================================

    /**
     * Zet automatisch de vervaldatum voor het opslaan.
     * SMS logs worden na 30 dagen automatisch verwijderd (batch job).
     *
     * Waarom 30 dagen?
     * - Recent genoeg voor troubleshooting
     * - Voldoende voor legal compliance
     * - Bespaart database opslag
     */
    @PrePersist
    private void setExpiryDate() {
        if (expiresAt == null && createdAt != null) {
            this.expiresAt = createdAt.plusDays(30);
        } else if (expiresAt == null) {
            // Fallback als createdAt nog niet gezet is
            this.expiresAt = OffsetDateTime.now().plusDays(30);
        }
    }

    // ============================================================
    // HELPER METHODS
    // ============================================================

    /**
     * Check of SMS succesvol verstuurd is.
     */
    public boolean isSuccessful() {
        return status == SmsStatus.SENT || status == SmsStatus.DELIVERED;
    }

    /**
     * Check of SMS gefaald is.
     */
    public boolean isFailed() {
        return status == SmsStatus.FAILED;
    }

    /**
     * Check of SMS nog in wachtrij staat.
     */
    public boolean isPending() {
        return status == SmsStatus.QUEUED;
    }
}