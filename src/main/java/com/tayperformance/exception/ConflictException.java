package com.tayperformance.exception;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Exception voor business conflicts (409).
 *
 * Use cases:
 * - Overlappende afspraken
 * - Duplicate entries
 * - Race conditions
 * - Business rule violations die conflicteren met bestaande data
 */
public class ConflictException extends RuntimeException {

    private final String code;
    private final Map<String, Object> details;

    // ============================================================
    // CONSTRUCTORS
    // ============================================================

    /**
     * Generic constructor met alleen message.
     */
    public ConflictException(String message) {
        super(message);
        this.code = "CONFLICT";
        this.details = new HashMap<>();
    }

    /**
     * Constructor met custom error code.
     */
    public ConflictException(String code, String message) {
        super(message);
        this.code = code;
        this.details = new HashMap<>();
    }

    /**
     * Constructor met code, message en details.
     */
    public ConflictException(String code, String message, Map<String, Object> details) {
        super(message);
        this.code = code;
        this.details = details != null ? details : new HashMap<>();
    }

    // ============================================================
    // FACTORY METHODS
    // ============================================================

    /**
     * Overlappende afspraak conflict.
     */
    public static ConflictException appointmentOverlap(
            Long conflictingAppointmentId,
            OffsetDateTime conflictStart,
            String carBrand) {

        Map<String, Object> details = new HashMap<>();
        details.put("conflictingAppointmentId", conflictingAppointmentId);
        details.put("conflictStart", conflictStart);
        details.put("carBrand", carBrand);

        return new ConflictException(
                "APPOINTMENT_OVERLAP",
                String.format("Staff heeft al een afspraak op dit tijdstip (%s - %s)",
                        conflictStart, carBrand),
                details
        );
    }

    /**
     * Duplicate phone number.
     */
    public static ConflictException duplicatePhone(String phone) {
        Map<String, Object> details = new HashMap<>();
        details.put("phone", phone);

        return new ConflictException(
                "DUPLICATE_PHONE",
                "Telefoonnummer is al in gebruik: " + phone,
                details
        );
    }

    /**
     * Duplicate username.
     */
    public static ConflictException duplicateUsername(String username) {
        Map<String, Object> details = new HashMap<>();
        details.put("username", username);

        return new ConflictException(
                "DUPLICATE_USERNAME",
                "Gebruikersnaam is al in gebruik: " + username,
                details
        );
    }

    /**
     * Duplicate service name.
     */
    public static ConflictException duplicateServiceName(String name) {
        Map<String, Object> details = new HashMap<>();
        details.put("serviceName", name);

        return new ConflictException(
                "DUPLICATE_SERVICE",
                "Service naam bestaat al: " + name,
                details
        );
    }

    /**
     * Appointment kan niet gewijzigd worden (verkeerde status).
     */
    public static ConflictException appointmentNotModifiable(Long id, String status) {
        Map<String, Object> details = new HashMap<>();
        details.put("appointmentId", id);
        details.put("currentStatus", status);

        return new ConflictException(
                "APPOINTMENT_NOT_MODIFIABLE",
                String.format("Afspraak kan niet gewijzigd worden (status: %s)", status),
                details
        );
    }

    // ============================================================
    // GETTERS
    // ============================================================

    public String getCode() {
        return code;
    }

    public Map<String, Object> getDetails() {
        return new HashMap<>(details); // Return copy voor immutability
    }
}