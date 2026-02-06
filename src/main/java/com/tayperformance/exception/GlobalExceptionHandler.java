package com.tayperformance.exception;

import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;

/**
 * Centralized exception handling voor alle REST endpoints.
 *
 * Logging strategie:
 * - 4xx: WARN
 * - Security: INFO/WARN
 * - 5xx: ERROR met stacktrace
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @Value("${spring.profiles.active:dev}")
    private String activeProfile;

    // ============================================================
    // BUSINESS EXCEPTIONS (4xx)
    // ============================================================

    /**
     * 409 Conflict – business conflicts (overlap, duplicate, race condition).
     */
    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ApiError> handleConflict(
            ConflictException ex,
            HttpServletRequest req) {

        log.warn("[CONFLICT] code={} message={} path={} details={}",
                ex.getCode(),
                ex.getMessage(),
                req.getRequestURI(),
                ex.getDetails());

        Map<String, Object> details = new HashMap<>();
        details.put("code", ex.getCode());

        if (ex.getDetails() != null && !ex.getDetails().isEmpty()) {
            details.put("conflict", ex.getDetails());
        }

        return build(HttpStatus.CONFLICT, ex.getMessage(), req, details);
    }

    /**
     * 404 Not Found.
     */
    @ExceptionHandler({ NotFoundException.class, EntityNotFoundException.class })
    public ResponseEntity<ApiError> handleNotFound(
            RuntimeException ex,
            HttpServletRequest req) {

        log.warn("[NOT_FOUND] message={} path={}", ex.getMessage(), req.getRequestURI());

        return build(
                HttpStatus.NOT_FOUND,
                ex.getMessage(),  // ✅ Werkt nu met public constructor
                req,
                Map.of("code", "NOT_FOUND")
        );
    }

    /**
     * 400 Bad Request – business rule fouten.
     */
    @ExceptionHandler({
            BadRequestException.class,
            IllegalArgumentException.class,
            IllegalStateException.class
    })
    public ResponseEntity<ApiError> handleBadRequest(
            RuntimeException ex,
            HttpServletRequest req) {

        log.warn("[BAD_REQUEST] message={} path={}", ex.getMessage(), req.getRequestURI());

        return build(
                HttpStatus.BAD_REQUEST,
                ex.getMessage(),
                req,
                Map.of("code", "BAD_REQUEST")
        );
    }

    /**
     * 400 Validation errors (@Valid).
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(
            MethodArgumentNotValidException ex,
            HttpServletRequest req) {

        Map<String, String> fieldErrors = new HashMap<>();
        ex.getBindingResult()
                .getFieldErrors()
                .forEach(err ->
                        fieldErrors.put(err.getField(), err.getDefaultMessage())
                );

        log.warn("[VALIDATION] path={} errors={}", req.getRequestURI(), fieldErrors);

        Map<String, Object> details = new HashMap<>();
        details.put("code", "VALIDATION_ERROR");
        details.put("fields", fieldErrors);

        return build(
                HttpStatus.BAD_REQUEST,
                "Validatie fout",
                req,
                details
        );
    }

    // ============================================================
    // SECURITY EXCEPTIONS
    // ============================================================

    /**
     * 401 Unauthorized – login mislukt.
     */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiError> handleBadCredentials(
            BadCredentialsException ex,
            HttpServletRequest req) {

        log.info("[AUTH] Bad credentials attempt from={}", req.getRemoteAddr());

        return build(
                HttpStatus.UNAUTHORIZED,
                "Ongeldige inloggegevens",
                req,
                Map.of("code", "BAD_CREDENTIALS")
        );
    }

    /**
     * 403 Forbidden – onvoldoende rechten.
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> handleAccessDenied(
            AccessDeniedException ex,
            HttpServletRequest req) {

        log.warn("[ACCESS_DENIED] path={} from={}",
                req.getRequestURI(),
                req.getRemoteAddr());

        return build(
                HttpStatus.FORBIDDEN,
                "Onvoldoende rechten voor deze actie",
                req,
                Map.of("code", "ACCESS_DENIED")
        );
    }

    // ============================================================
    // DATABASE EXCEPTIONS
    // ============================================================

    /**
     * 409 – Database constraint violation.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> handleDataIntegrity(
            DataIntegrityViolationException ex,
            HttpServletRequest req) {

        String rootCause = extractRootCause(ex);

        log.warn("[DB_CONSTRAINT] path={} cause={}",
                req.getRequestURI(),
                rootCause);

        Map<String, Object> details = new HashMap<>();
        details.put("code", "DATA_INTEGRITY_VIOLATION");

        if (isDevelopment()) {
            details.put("dbError", rootCause);
        }

        return build(
                HttpStatus.CONFLICT,
                "Database constraint violation. Mogelijk duplicaat of ongeldige referentie.",
                req,
                details
        );
    }

    // ============================================================
    // CATCH-ALL (500)
    // ============================================================

    /**
     * 500 Internal Server Error.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleAll(
            Exception ex,
            HttpServletRequest req) {

        log.error("[INTERNAL_ERROR] path={} error={}",
                req.getRequestURI(),
                ex.getMessage(),
                ex);

        Map<String, Object> details = new HashMap<>();
        details.put("code", "INTERNAL_ERROR");

        String message;
        if (isDevelopment()) {
            message = "Internal server error: " + ex.getMessage();
            details.put("exception", ex.getClass().getSimpleName());
        } else {
            message = "Er is iets misgegaan. Probeer het later opnieuw.";
        }

        return build(
                HttpStatus.INTERNAL_SERVER_ERROR,
                message,
                req,
                details
        );
    }

    // ============================================================
    // HELPERS
    // ============================================================

    private ResponseEntity<ApiError> build(
            HttpStatus status,
            String message,
            HttpServletRequest req,
            Map<String, Object> details) {

        ApiError body = new ApiError(
                OffsetDateTime.now(ZoneOffset.UTC),
                status.value(),
                status.getReasonPhrase(),
                message,
                req.getRequestURI(),
                details
        );

        return ResponseEntity
                .status(status)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body);
    }

    private String extractRootCause(Throwable t) {
        Throwable current = t;
        int depth = 0;

        while (current.getCause() != null
                && current.getCause() != current
                && depth < 10) {

            current = current.getCause();
            depth++;
        }

        return current.getMessage() != null
                ? current.getMessage()
                : current.getClass().getSimpleName();
    }

    private boolean isDevelopment() {
        return activeProfile != null &&
                (activeProfile.contains("dev")
                        || activeProfile.contains("local"));
    }
}
