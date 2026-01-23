package com.tayperformance.exception;

import lombok.Getter;
import java.time.OffsetDateTime;

@Getter
public class ConflictException extends RuntimeException {
    private final Long conflictId;
    private final OffsetDateTime startTime;
    private final String carBrand;

    public ConflictException(String message, Long conflictId, OffsetDateTime start, String brand) {
        super(message);
        this.conflictId = conflictId;
        this.startTime = start;
        this.carBrand = brand;
    }
}
