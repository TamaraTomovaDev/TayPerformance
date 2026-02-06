package com.tayperformance.exception;

/**
 * Exception voor niet-gevonden entiteiten (404).
 * Bevat generieke en entity-specifieke factory-methods.
 */
public class NotFoundException extends RuntimeException {

    // ============================================================
    // CONSTRUCTORS
    // ============================================================

    public NotFoundException(String message) {
        super(message);
    }

    public NotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    // ============================================================
    // GENERIC FACTORY METHOD
    // ============================================================

    /**
     * Algemene not-found builder voor entiteiten.
     * Voorbeeld: NotFoundException.of("Customer", id)
     */
    public static NotFoundException of(String entity, Object id) {
        return new NotFoundException(entity + " niet gevonden met id=" + id);
    }

    // ============================================================
    // SPECIFIEKE FACTORY METHODS
    // ============================================================

    public static NotFoundException appointment(Long id) {
        return of("Afspraak", id);
    }

    public static NotFoundException customer(Long id) {
        return of("Klant", id);
    }

    public static NotFoundException customerByPhone(String phone) {
        return new NotFoundException("Klant niet gevonden met telefoonnummer: " + phone);
    }

    public static NotFoundException service(Long id) {
        return of("Service", id);
    }

    public static NotFoundException user(Long id) {
        return of("Gebruiker", id);
    }

    public static NotFoundException staff(Long id) {
        return of("Staff member", id);
    }

    public static NotFoundException auditLog(Long id) {
        return of("Audit log", id);
    }
}