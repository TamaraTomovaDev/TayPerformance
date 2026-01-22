package com.tayperformance.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Wordt gebruikt wanneer een business-conflict optreedt
 * (bv. dubbele afspraak in hetzelfde tijdslot)
 */
@ResponseStatus(HttpStatus.CONFLICT)
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }
}
