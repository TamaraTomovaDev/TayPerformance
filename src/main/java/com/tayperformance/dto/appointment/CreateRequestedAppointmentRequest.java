package com.tayperformance.dto.appointment;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
public class CreateRequestedAppointmentRequest {

    @NotBlank(message = "Telefoonnummer is verplicht")
    private String customerPhone;

    private String customerName;

    @NotBlank(message = "Automerk is verplicht")
    private String carBrand;

    private String carModel;

    private String description;

    @NotNull(message = "Starttijd is verplicht")
    @Future(message = "Afspraak moet in de toekomst liggen")
    private OffsetDateTime startTime;

    // klant kiest een service -> defaultMinutes gebruiken voor placeholder endTime
    @NotNull(message = "Service is verplicht")
    private Long serviceId;
}
