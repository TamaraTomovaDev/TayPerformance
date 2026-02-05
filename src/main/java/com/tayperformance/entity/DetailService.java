package com.tayperformance.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;

/**
 * Detailing Service entity - Beschrijft een aangeboden service met timing.
 *
 * Voorbeelden:
 * - Basis Wash (min: 30, default: 45, max: 60)
 * - Full Detail (min: 120, default: 180, max: 240)
 * - Ceramic Coating (min: 180, default: 240, max: 300)
 *
 * Min/max/default minuten stellen flexibiliteit in planning mogelijk:
 * - Min: snelste scenario (bijvoorbeeld: kleine auto)
 * - Default: gemiddelde duur (normale auto)
 * - Max: langste scenario (bijvoorbeeld: grote SUV of extra vuile auto)
 */
@Entity
@Table(
        name = "services",
        indexes = {
                @Index(name = "idx_services_active", columnList = "active"),
                @Index(name = "idx_services_name", columnList = "name")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class DetailService {

    @EqualsAndHashCode.Include
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Versie voor optimistic locking.
     * Voorkomt race conditions bij wijzigen van services.
     */
    @Version
    private Long version;

    // ============================================================
    // SERVICE INFORMATIE
    // ============================================================

    /**
     * Naam van de service.
     * Bijvoorbeeld: "Volledige Detailing", "Interieur Reiniging", "Ceramic Coating"
     */
    @NotBlank(message = "Service naam is verplicht")
    @Size(min = 3, max = 120, message = "Service naam moet tussen 3 en 120 karakters zijn")
    @Column(nullable = false, length = 120)
    private String name;

    /**
     * Gedetailleerde beschrijving van de service.
     * Bijvoorbeeld: wat er precies gedaan wordt, welke producten gebruikt worden.
     * Wordt getoond op de publieke website.
     */
    @Size(max = 5000, message = "Beschrijving mag maximaal 5000 karakters zijn")
    @Column(columnDefinition = "TEXT")
    private String description;

    // ============================================================
    // TIMING (in minuten)
    // ============================================================

    /**
     * Minimale duur van deze service in minuten.
     * Bijvoorbeeld: snelle wash bij kleine auto = 30 minuten.
     */
    @NotNull(message = "Minimum duur is verplicht")
    @Min(value = 15, message = "Minimum duur moet minstens 15 minuten zijn")
    @Column(name = "min_minutes", nullable = false)
    private Integer minMinutes;

    /**
     * Maximale duur van deze service in minuten.
     * Bijvoorbeeld: volledige detail bij grote SUV = 240 minuten.
     */
    @NotNull(message = "Maximum duur is verplicht")
    @Max(value = 480, message = "Maximum duur mag niet meer dan 8 uur (480 minuten) zijn")
    @Column(name = "max_minutes", nullable = false)
    private Integer maxMinutes;

    /**
     * Standaard duur van deze service in minuten.
     * Dit is de duur die voorgesteld wordt bij het maken van een afspraak.
     * Moet tussen min en max liggen (validatie zie onder).
     */
    @NotNull(message = "Standaard duur is verplicht")
    @Column(name = "default_minutes", nullable = false)
    private Integer defaultMinutes;

    // ============================================================
    // STATUS
    // ============================================================

    /**
     * Actieve status van de service.
     *
     * false = Service niet meer beschikbaar:
     * - Seizoensgebonden service
     * - Service tijdelijk niet aangeboden
     * - Service uitgefaseerd
     *
     * Voordeel: Oude afspraken blijven refereren naar service.
     */
    @Builder.Default
    @Column(nullable = false)
    private boolean active = true;

    // ============================================================
    // AUDIT TIMESTAMPS
    // ============================================================

    /**
     * Aanmaakdatum van de service.
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    /**
     * Laatste wijzigingsdatum van de service.
     */
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    // ============================================================
    // CUSTOM VALIDATIE
    // ============================================================

    /**
     * Valideert dat de standaard duur binnen het min/max bereik ligt.
     * Bijvoorbeeld: als min=30 en max=120, dan moet default tussen 30-120 zijn.
     */
    @AssertTrue(message = "Standaard duur moet tussen minimum en maximum duur liggen")
    private boolean isDefaultWithinRange() {
        if (defaultMinutes == null || minMinutes == null || maxMinutes == null) {
            return true; // Laat andere validaties nulls afhandelen
        }
        return defaultMinutes >= minMinutes && defaultMinutes <= maxMinutes;
    }

    /**
     * Valideert dat maximum groter is dan minimum.
     * Voorkomt ongeldige configuratie.
     */
    @AssertTrue(message = "Maximum duur moet groter zijn dan minimum duur")
    private boolean isMaxGreaterThanMin() {
        if (minMinutes == null || maxMinutes == null) {
            return true;
        }
        return maxMinutes > minMinutes;
    }
}