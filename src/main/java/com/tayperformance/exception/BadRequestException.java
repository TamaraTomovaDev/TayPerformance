package com.tayperformance.exception;

/**
 * Exception voor business rule violations (400).
 *
 * Use cases:
 * - Invalid input data (buiten validation annotations)
 * - Business rule violations
 * - Illegal state transitions
 */
public class BadRequestException extends RuntimeException {

    // ============================================================
    // CONSTRUCTORS
    // ============================================================

    public BadRequestException(String message) {
        super(message);
    }

    public BadRequestException(String message, Throwable cause) {
        super(message, cause);
    }

    // ============================================================
    // FACTORY METHODS (optioneel - voor veelgebruikte cases)
    // ============================================================

    public static BadRequestException invalidDateRange(String detail) {
        return new BadRequestException("Ongeldige datumbereik: " + detail);
    }

    public static BadRequestException invalidStatus(String currentStatus, String targetStatus) {
        return new BadRequestException(
                String.format("Kan status niet wijzigen van %s naar %s", currentStatus, targetStatus)
        );
    }

    public static BadRequestException missingRequiredField(String fieldName) {
        return new BadRequestException("Verplicht veld ontbreekt: " + fieldName);
    }

    public static BadRequestException invalidDuration(int minutes) {
        return new BadRequestException(
                String.format("Ongeldige duur: %d minuten (moet tussen 15 en 480 zijn)", minutes)
        );
    }

    public static BadRequestException pastDate(String fieldName) {
        return new BadRequestException(
                String.format("%s moet in de toekomst liggen", fieldName)
        );
    }
}