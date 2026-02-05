package com.tayperformance.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;

/**
 * Gebruiker entity - Voor interne garage medewerkers (NIET voor klanten).
 *
 * Rollen:
 * - ADMIN: Zaakvoerder - volledige toegang, gebruikersbeheer, rapportages
 * - STAFF: Medewerker - afspraken beheren, klanten beheren, SMS versturen
 *
 * Klanten krijgen GEEN account - ze boeken via publieke website zonder login.
 */
@Entity
@Table(
        name = "users",
        indexes = {
                @Index(name = "idx_users_username", columnList = "username"),
                @Index(name = "idx_users_active_role", columnList = "active,role")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"passwordHash"}) // ⚠️ SECURITY: Password NOOIT in logs
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class User {

    @EqualsAndHashCode.Include
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Versie voor optimistic locking.
     * Voorkomt concurrency problemen bij gelijktijdige wijzigingen.
     */
    @Version
    private Long version;

    // ============================================================
    // AUTHENTICATIE
    // ============================================================

    /**
     * Gebruikersnaam (= email adres).
     *
     * Gebruikt als login identifier.
     * Moet geldig email formaat zijn voor eventuele email notificaties.
     */
    @NotBlank(message = "Gebruikersnaam is verplicht")
    @Email(message = "Gebruikersnaam moet een geldig email adres zijn")
    @Size(max = 80, message = "Gebruikersnaam mag maximaal 80 karakters zijn")
    @Column(unique = true, nullable = false, length = 80)
    private String username;

    /**
     * Wachtwoord hash (BCrypt).
     *
     * ⚠️ SECURITY CRITICAL:
     * - NOOIT plain text wachtwoord opslaan
     * - Gebruik BCryptPasswordEncoder met strength 12+
     * - @JsonIgnore voorkomt serialization in API responses
     * - ToString exclude voorkomt logging in stacktraces
     */
    @NotBlank(message = "Wachtwoord is verplicht")
    @JsonIgnore
    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    // ============================================================
    // AUTORISATIE
    // ============================================================

    /**
     * Rol van de gebruiker.
     *
     * ADMIN: Volledige toegang
     * - Gebruikersbeheer (CRUD)
     * - Alle afspraken zien/beheren
     * - Audit logs inzien
     * - Rapportages genereren
     * - Systeeminstellingen wijzigen
     *
     * STAFF: Beperkte toegang
     * - Afspraken beheren (CRUD)
     * - Klanten beheren (CRUD)
     * - SMS versturen
     * - GEEN toegang tot gebruikersbeheer
     * - GEEN toegang tot audit logs
     */
    @NotNull(message = "Rol is verplicht")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    // ============================================================
    // STATUS
    // ============================================================

    /**
     * Actieve status van de gebruiker.
     *
     * false = Account gedeactiveerd:
     * - Medewerker niet meer in dienst
     * - Tijdelijke deactivatie
     * - Security incident
     *
     * Voordeel soft delete: Audit trail blijft intact.
     * Oude afspraken tonen nog steeds wie ze aanmaakte.
     */
    @Builder.Default
    @Column(nullable = false)
    private boolean active = true;

    // ============================================================
    // AUDIT TIMESTAMPS
    // ============================================================

    /**
     * Registratiedatum van het account.
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    /**
     * Laatste wijziging van het account.
     * Bijgewerkt bij password change, rol wijziging, etc.
     */
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    // ============================================================
    // HELPER METHODS
    // ============================================================

    /**
     * Check of gebruiker ADMIN rol heeft.
     * Handig voor autorisatie checks in code.
     */
    public boolean isAdmin() {
        return this.role == Role.ADMIN;
    }

    /**
     * Check of gebruiker STAFF rol heeft.
     */
    public boolean isStaff() {
        return this.role == Role.STAFF;
    }

    /**
     * Geeft display naam terug voor UI.
     * Bijvoorbeeld: "admin@tayperformance.com (ADMIN)"
     */
    public String getDisplayName() {
        return username + " (" + role.name() + ")";
    }
}