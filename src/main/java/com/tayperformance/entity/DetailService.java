package com.tayperformance.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * DetailService = type dienst (tinting, detailing, etc.)
 * Wordt gebruikt om:
 * - naam te tonen
 * - default duur te bepalen (voor public bookings)
 * - basisprijs te tonen (optioneel)
 */
@Entity
@Table(
        name = "detail_services",
        indexes = {
                @Index(name = "idx_service_active", columnList = "active"),
                @Index(name = "idx_service_name", columnList = "name")
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

    @Version
    private Long version;

    @NotBlank(message = "Naam is verplicht")
    @Size(min = 2, max = 120, message = "Naam moet tussen 2 en 120 karakters zijn")
    @Column(nullable = false, length = 120)
    private String name;

    /**
     * Default duur in minuten (wordt gebruikt voor public bookings).
     */
    @NotNull(message = "Default duur is verplicht")
    @Min(value = 15, message = "Minimaal 15 minuten")
    @Max(value = 480, message = "Maximaal 8 uur")
    @Column(name = "default_minutes", nullable = false)
    private Integer defaultMinutes;

    /**
     * Basisprijs (optioneel). Interne staff kan echte price instellen per appointment.
     */
    @Digits(integer = 8, fraction = 2, message = "Ongeldige prijs")
    @DecimalMin(value = "0.00", inclusive = true, message = "Prijs moet >= 0 zijn")
    @Column(name = "base_price", precision = 10, scale = 2)
    private BigDecimal basePrice;

    @Builder.Default
    @Column(nullable = false)
    private boolean active = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}
