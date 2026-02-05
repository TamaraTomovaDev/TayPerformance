package com.tayperformance.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.OffsetDateTime;

/**
 * Afspraak (Appointment) - Aggregate root.
 *
 * Representeert een geplande detailing service met timing, prijs,
 * status tracking en audit informatie.
 *
 * Gebruikt optimistic locking (@Version) om concurrency problemen te voorkomen.
 * Staff bepaalt zelf start EN eindtijd voor maximale flexibiliteit.
 */
@Entity
@Table(
        name = "appointments",
        indexes = {
                @Index(name = "idx_appt_start_time", columnList = "start_time"),
                @Index(name = "idx_appt_status", columnList = "status"),
                @Index(name = "idx_appt_customer_start", columnList = "customer_id,start_time"),
                @Index(name = "idx_appt_assigned_start", columnList = "assigned_staff_id,start_time")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"customer", "service", "assignedStaff", "createdBy", "updatedBy"})
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Appointment {

    @EqualsAndHashCode.Include
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Versie voor optimistic locking.
     * Voorkomt dat twee gebruikers tegelijk dezelfde afspraak wijzigen.
     */
    @Version
    private Long version;

    // ============================================================
    // RELATIES
    // ============================================================

    /**
     * Klant die de afspraak heeft gemaakt (verplicht).
     * Lazy loading voor betere performance.
     */
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    @JsonIgnore
    private Customer customer;

    /**
     * Gekozen detailing service (optioneel).
     * Kan null zijn voor custom afspraken.
     * Gebruikt als suggestie voor duur, maar staff bepaalt eindtijd zelf.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_id")
    @JsonIgnore
    private DetailService service;

    /**
     * Toegewezen medewerker (optioneel).
     * Null = nog niet toegewezen.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_staff_id")
    @JsonIgnore
    private User assignedStaff;

    /**
     * Gebruiker die deze afspraak aanmaakte.
     * Voor audit trail - wie heeft ingeboekt?
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_user_id")
    @JsonIgnore
    private User createdBy;

    /**
     * Gebruiker die als laatste wijzigde.
     * Voor audit trail - wie deed de laatste aanpassing?
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by_user_id")
    @JsonIgnore
    private User updatedBy;

    // ============================================================
    // VOERTUIG & KLANT INPUT
    // ============================================================

    /**
     * Merk van de auto (verplicht).
     * Bijvoorbeeld: "BMW", "Audi", "Mercedes"
     */
    @NotBlank
    @Size(min = 2, max = 80)
    @Column(name = "car_brand", nullable = false, length = 80)
    private String carBrand;

    /**
     * Model van de auto (optioneel).
     * Bijvoorbeeld: "M3", "A4", "C-Class"
     */
    @Size(max = 80)
    @Column(name = "car_model", length = 80)
    private String carModel;

    /**
     * Beschrijving van gewenste werkzaamheden.
     * Vrije tekst voor specifieke wensen.
     * Bijvoorbeeld: "Volledige poetsdienst + lederen zetels behandelen"
     */
    @Size(max = 5000)
    @Column(columnDefinition = "TEXT")
    private String description;

    // ============================================================
    // PLANNING
    // ============================================================

    /**
     * Starttijd van de afspraak.
     * Staff bepaalt zelf wanneer de afspraak begint.
     * Inclusief tijdzone (OffsetDateTime).
     */
    @NotNull
    @Column(name = "start_time", nullable = false)
    private OffsetDateTime startTime;

    /**
     * Eindtijd van de afspraak.
     * Staff bepaalt zelf wanneer de afspraak eindigt.
     *
     * Voordelen manuele eindtijd:
     * - Staff kan rekening houden met complexiteit
     * - Buffer tijd tussen afspraken mogelijk
     * - Flexibel aanpassen aan situatie
     */
    @NotNull
    @Column(name = "end_time", nullable = false)
    private OffsetDateTime endTime;

    /**
     * Berekende duur in minuten.
     * Wordt NIET opgeslagen (redundante data vermijden).
     *
     * @Transient = niet persisteren in database
     * @return Duur in minuten, of null als start/end niet gezet
     */
    @Transient
    public Integer getDurationMinutes() {
        if (startTime == null || endTime == null) {
            return null;
        }
        return (int) Duration.between(startTime, endTime).toMinutes();
    }

    // ============================================================
    // BUSINESS GEGEVENS
    // ============================================================

    /**
     * Afgesproken prijs voor deze afspraak.
     * BigDecimal voor nauwkeurige financiële berekeningen.
     * Kan null zijn als prijs nog niet bepaald is.
     */
    @DecimalMin("0.01")
    @Digits(integer = 8, fraction = 2)
    @Column(precision = 10, scale = 2)
    private BigDecimal price;

    /**
     * Status van de afspraak.
     * Default = REQUESTED (aangevraagd via website).
     */
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private AppointmentStatus status = AppointmentStatus.REQUESTED;

    // ============================================================
    // AUDIT TIMESTAMPS
    // ============================================================

    /**
     * Aanmaakdatum - automatisch gezet door Hibernate.
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    /**
     * Laatste wijziging - automatisch bijgewerkt.
     */
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    // ============================================================
    // BUSINESS VALIDATIE
    // ============================================================

    /**
     * Valideert dat eindtijd na starttijd ligt.
     * Dit is de enige essentiële timing validatie.
     */
    @AssertTrue(message = "Eindtijd moet na starttijd liggen")
    private boolean isEndTimeAfterStartTime() {
        return startTime == null || endTime == null || endTime.isAfter(startTime);
    }

    /**
     * Valideert dat afspraak niet langer dan 8 uur duurt.
     * Voorkomt typfouten bij invoeren van tijden.
     */
    @AssertTrue(message = "Afspraak duurt langer dan 8 uur - controleer de tijden")
    private boolean isDurationReasonable() {
        if (startTime == null || endTime == null) {
            return true;
        }
        return Duration.between(startTime, endTime).toHours() <= 8;
    }

    // ============================================================
    // HELPER METHODS
    // ============================================================

    /**
     * Check of deze afspraak in het verleden ligt.
     * Handig voor business logic validatie.
     */
    @Transient
    public boolean isPast() {
        return startTime != null && startTime.isBefore(OffsetDateTime.now());
    }

    /**
     * Check of deze afspraak vandaag is.
     */
    @Transient
    public boolean isToday() {
        if (startTime == null) return false;
        OffsetDateTime now = OffsetDateTime.now();
        return startTime.toLocalDate().equals(now.toLocalDate());
    }

    /**
     * Check of deze afspraak in de toekomst ligt.
     */
    @Transient
    public boolean isUpcoming() {
        return startTime != null && startTime.isAfter(OffsetDateTime.now());
    }

    /**
     * Check of deze afspraak nog gewijzigd mag worden.
     *
     * Niet wijzigbaar als:
     * - Status = COMPLETED (afgewerkt)
     * - Status = NOSHOW (niet verschenen)
     *
     * Let op: Afspraken in verleden kunnen WEL gewijzigd worden
     * (bijvoorbeeld status update van CONFIRMED naar COMPLETED).
     */
    @Transient
    public boolean isModifiable() {
        return status != AppointmentStatus.COMPLETED &&
                status != AppointmentStatus.NOSHOW;
    }

    /**
     * Check of deze afspraak actief is (in planning).
     * Voor kalender filtering.
     */
    @Transient
    public boolean isActive() {
        return status == AppointmentStatus.REQUESTED ||
                status == AppointmentStatus.CONFIRMED ||
                status == AppointmentStatus.IN_PROGRESS;
    }

    /**
     * Check of deze afspraak succesvol is afgerond.
     */
    @Transient
    public boolean isCompleted() {
        return status == AppointmentStatus.COMPLETED;
    }

    /**
     * Check of deze afspraak geannuleerd is.
     */
    @Transient
    public boolean isCanceled() {
        return status == AppointmentStatus.CANCELED;
    }
}