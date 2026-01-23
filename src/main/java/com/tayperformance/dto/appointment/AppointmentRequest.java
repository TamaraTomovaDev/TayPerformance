package com.tayperformance.dto.appointment;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Data
public class AppointmentRequest {

    @NotBlank(message = "Telefoonnummer is verplicht")
    private String customerPhone;

    private String customerName;

    @NotBlank(message = "Automerk is verplicht")
    private String carBrand;

    private String carModel;

    private String description;

    @NotNull(message = "Prijs is verplicht")
    @DecimalMin(value = "0.0", inclusive = true)
    private BigDecimal price;

    @NotNull(message = "Starttijd is verplicht")
    @Future(message = "Afspraak moet in de toekomst liggen")
    private OffsetDateTime startTime;

    @NotNull(message = "Eindtijd is verplicht")
    private OffsetDateTime endTime;
}
