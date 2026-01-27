package com.tayperformance.exception;

import lombok.Getter;

import java.time.OffsetDateTime;
import java.util.Map;

@Getter
public class ConflictException extends RuntimeException {

    private final String code; // bv "APPOINTMENT_OVERLAP"
    private final Map<String, Object> details;

    public ConflictException(String message, String code, Map<String, Object> details) {
        super(message);
        this.code = code;
        this.details = details;
    }

    public static ConflictException appointmentOverlap(Long conflictId, OffsetDateTime startTime, String carBrand) {
        return new ConflictException(
                "Overlapping afspraak. Kies een ander tijdstip of medewerker.",
                "APPOINTMENT_OVERLAP",
                Map.of(
                        "appointmentId", conflictId,
                        "startTime", startTime,
                        "carBrand", carBrand
                )
        );
    }
}
