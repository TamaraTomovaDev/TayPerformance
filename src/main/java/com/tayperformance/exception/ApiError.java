package com.tayperformance.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.Map;

/**
 * Standaard error response format voor alle API errors.
 *
 * Voorbeeld response:
 * {
 *   "timestamp": "2026-02-06T10:30:00Z",
 *   "status": 404,
 *   "error": "Not Found",
 *   "message": "Afspraak niet gevonden met id=123",
 *   "path": "/api/appointments/123",
 *   "details": {
 *     "code": "NOT_FOUND"
 *   }
 * }
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiError {

    /**
     * Timestamp van de error (UTC).
     */
    private OffsetDateTime timestamp;

    /**
     * HTTP status code (400, 404, 409, 500, etc.).
     */
    private int status;

    /**
     * HTTP status reason phrase ("Bad Request", "Not Found", etc.).
     */
    private String error;

    /**
     * User-friendly error message in Nederlands.
     */
    private String message;

    /**
     * Request path waar de error optrad.
     */
    private String path;

    /**
     * Extra details (optioneel).
     * Kan bevatten: error code, field errors, conflict details, etc.
     */
    private Map<String, Object> details;
}