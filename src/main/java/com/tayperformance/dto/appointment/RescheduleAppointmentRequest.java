// src/main/java/com/tayperformance/dto/appointment/RescheduleAppointmentRequest.java
package com.tayperformance.dto.appointment;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
public class RescheduleAppointmentRequest {

    @NotNull(message = "Nieuwe starttijd is verplicht")
    @Future(message = "Nieuwe starttijd moet in de toekomst liggen")
    private OffsetDateTime newStartTime;

    @NotNull(message = "Nieuwe duur is verplicht")
    @Min(value = 15, message = "Duur moet minstens 15 minuten zijn")
    private Integer durationMinutes;

    @NotNull(message = "Staff is verplicht")
    private Long assignedStaffId;
}
