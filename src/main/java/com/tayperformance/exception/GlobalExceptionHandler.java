package com.tayperformance.exception;

import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ApiError> handleConflict(ConflictException ex, HttpServletRequest req) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), req, Map.of(
                "code", ex.getCode(),
                "conflict", ex.getDetails()
        ));
    }

    @ExceptionHandler({NotFoundException.class, EntityNotFoundException.class})
    public ResponseEntity<ApiError> handleNotFound(RuntimeException ex, HttpServletRequest req) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), req, null);
    }

    @ExceptionHandler({BadRequestException.class, IllegalArgumentException.class, IllegalStateException.class})
    public ResponseEntity<ApiError> handleBadRequest(RuntimeException ex, HttpServletRequest req) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), req, null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest req) {
        Map<String, Object> details = new HashMap<>();
        Map<String, String> fieldErrors = new HashMap<>();

        ex.getBindingResult().getFieldErrors()
                .forEach(err -> fieldErrors.put(err.getField(), err.getDefaultMessage()));

        details.put("code", "VALIDATION_ERROR");
        details.put("fields", fieldErrors);

        return build(HttpStatus.BAD_REQUEST, "Validatie fout", req, details);
    }

    /**
     * Voor database constraint errors.
     * Bij Postgres EXCLUDE overlap kan dit hier binnenkomen.
     * Tip: als je de constraint name checkt, kan je er 409 van maken.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> handleDataIntegrity(DataIntegrityViolationException ex, HttpServletRequest req) {
        // Default: 409 is vaak correct voor unique/constraint issues
        return build(HttpStatus.CONFLICT, "Database conflict", req, Map.of(
                "code", "DATA_INTEGRITY_VIOLATION"
        ));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleAll(Exception ex, HttpServletRequest req) {
        // In prod: geen details lekken
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Er is iets misgegaan", req, Map.of(
                "code", "INTERNAL_ERROR"
        ));
    }

    private ResponseEntity<ApiError> build(HttpStatus status, String message, HttpServletRequest req, Map<String, Object> details) {
        ApiError body = new ApiError(
                OffsetDateTime.now(ZoneOffset.UTC),
                status.value(),
                status.getReasonPhrase(),
                message,
                req.getRequestURI(),
                details
        );
        return ResponseEntity.status(status).contentType(MediaType.APPLICATION_JSON).body(body);
    }
}
