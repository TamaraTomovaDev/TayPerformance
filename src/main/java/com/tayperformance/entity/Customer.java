package com.tayperformance.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Klant entity - Bevat minimale klantgegevens volgens privacy-by-design principe.
 *
 * Belangrijke keuzes:
 * - Telefoonnummer is VERPLICHT en UNIEK (primary identifier)
 * - Naam is OPTIONEEL (niet altijd nodig voor detailing service)
 * - Geen adres, geen email (minimalisatie persoonsgegevens)
 * - Active flag voor soft delete (GDPR right to be forgotten)
 *
 * De relatie met Appointment gebruikt GEEN CascadeType.ALL om data verlies te voorkomen.
 */
@Entity
@Table(
        name = "customers",
        indexes = {
                @Index(name = "idx_customers_phone", columnList = "phone"),
                @Index(name = "idx_customers_active", columnList = "active")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"appointments"})
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Customer {

    @EqualsAndHashCode.Include
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Versie voor optimistic locking.
     * Belangrijk bij gelijktijdige updates van klantgegevens.
     */
    @Version
    private Long version;

    // ============================================================
    // IDENTIFICATIE
    // ============================================================

    /**
     * Telefoonnummer van de klant (VERPLICHT en UNIEK).
     *
     * Dit is de primary business identifier omdat:
     * - Nodig voor SMS communicatie
     * - Klanten bellen vaak om afspraak te maken
     * - Geen email vereist (simpliciteit)
     *
     * Formaat: E.164 standaard (bijvoorbeeld: +33612345678)
     * BELANGRIJK: Normaliseer ALTIJD in de service layer voor opslaan!
     */
    @NotBlank(message = "Telefoonnummer is verplicht")
    @Pattern(
            regexp = "^\\+?[1-9]\\d{1,14}$",
            message = "Ongeldig telefoonnummer (gebruik E.164 formaat, bijvoorbeeld: +33612345678)"
    )
    @Column(unique = true, nullable = false, length = 30)
    private String phone;

    // ============================================================
    // PERSOONLIJKE GEGEVENS (OPTIONEEL)
    // ============================================================

    /**
     * Voornaam van de klant (OPTIONEEL).
     * Niet verplicht omdat:
     * - Niet altijd nodig voor service
     * - Privacy-by-design principe
     * - SMS kan verstuurd worden met enkel telefoonnummer
     */
    @Size(max = 80, message = "Voornaam mag maximaal 80 karakters zijn")
    @Column(name = "first_name", length = 80)
    private String firstName;

    /**
     * Achternaam van de klant (OPTIONEEL).
     * Zelfde reden als firstName - niet altijd nodig.
     */
    @Size(max = 80, message = "Achternaam mag maximaal 80 karakters zijn")
    @Column(name = "last_name", length = 80)
    private String lastName;

    // ============================================================
    // STATUS
    // ============================================================

    /**
     * Actieve status van de klant.
     *
     * false = Soft delete / GDPR verwijdering
     * - Klant heeft recht op vergetelheid aangevraagd
     * - Klant is geblokkeerd wegens no-shows
     *
     * Voordeel: Historische data blijft behouden voor rapportage.
     */
    @Builder.Default
    @Column(nullable = false)
    private boolean active = true;

    // ============================================================
    // AUDIT TIMESTAMPS
    // ============================================================

    /**
     * Registratiedatum van de klant.
     * Automatisch gezet bij eerste afspraak.
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    /**
     * Laatste update van klantgegevens.
     * Wordt bijgewerkt bij wijziging telefoon of naam.
     */
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    // ============================================================
    // RELATIES
    // ============================================================

    /**
     * Alle afspraken van deze klant.
     *
     * BELANGRIJKE CASCADE KEUZE:
     * - ALLEEN CascadeType.PERSIST (nieuwe afspraak opslaan samen met klant)
     * - GEEN CascadeType.ALL of REMOVE
     * - orphanRemoval = false
     *
     * Waarom? Bij verwijderen van klant willen we afspraken BEHOUDEN:
     * - Voor audit trail
     * - Voor financiële rapportage
     * - Voor historische data
     *
     * In plaats daarvan: gebruik soft delete (active = false)
     */
    @Builder.Default
    @OneToMany(mappedBy = "customer", cascade = {CascadeType.PERSIST}, orphanRemoval = false)
    @JsonIgnore
    private List<Appointment> appointments = new ArrayList<>();

    // ============================================================
    // HELPER METHODS
    // ============================================================

    /**
     * Helper methode om afspraak toe te voegen met bidirectionele relatie.
     * Zorgt ervoor dat beide kanten van de relatie consistent zijn.
     */
    public void addAppointment(Appointment appointment) {
        appointments.add(appointment);
        appointment.setCustomer(this);
    }

    /**
     * Helper methode om afspraak te verwijderen met bidirectionele relatie.
     */
    public void removeAppointment(Appointment appointment) {
        appointments.remove(appointment);
        appointment.setCustomer(null);
    }

    /**
     * Geeft volledige naam terug indien beschikbaar.
     * Anders alleen telefoonnummer.
     */
    public String getDisplayName() {
        if (firstName != null && lastName != null) {
            return firstName + " " + lastName;
        } else if (firstName != null) {
            return firstName;
        } else {
            return phone; // Fallback naar telefoonnummer
        }
    }
}