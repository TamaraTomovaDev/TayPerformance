package com.tayperformance.dto.appointment;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.time.OffsetDateTime;

@Data
public class RescheduleAppointmentRequest {

    @NotNull(message = "Nieuwe starttijd is verplicht")
    @Future(message = "Nieuwe starttijd moet in de toekomst liggen")
    private OffsetDateTime newStartTime;
}