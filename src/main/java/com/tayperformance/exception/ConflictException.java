package com.tayperformance.exception;

import lombok.Getter;

import java.time.OffsetDateTime;
import java.util.Map;

@Getter
public class ConflictException extends RuntimeException {

    private final String code;
    private final Map<String, Object> details;

    public ConflictException(String code, String message, Map<String, Object> details) {
        super(message);
        this.code = code;
        this.details = details;
    }

    public static ConflictException appointmentOverlap(Long otherId, OffsetDateTime otherStart, String otherCarBrand) {
        return new ConflictException(
                "APPOINTMENT_OVERLAP",
                "Er is al een afspraak op dit tijdstip voor deze medewerker",
                Map.of(
                        "conflictingAppointmentId", otherId,
                        "conflictingStartTime", otherStart,
                        "conflictingCarBrand", otherCarBrand
                )
        );
    }
}
