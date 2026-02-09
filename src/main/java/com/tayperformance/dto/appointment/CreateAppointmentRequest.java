package com.tayperformance.dto.appointment;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Data
public class CreateAppointmentRequest {

    @NotBlank(message = "Telefoonnummer is verplicht")
    private String customerPhone;

    private String customerName;

    @NotBlank(message = "Automerk is verplicht")
    private String carBrand;

    private String carModel;

    @NotBlank(message = "Omschrijving is verplicht")
    private String description;

    @NotNull(message = "Starttijd is verplicht")
    @Future(message = "Starttijd moet in de toekomst liggen")
    private OffsetDateTime startTime;

    @Positive(message = "Prijs moet positief zijn")
    private BigDecimal price;

    @Min(value = 15, message = "Minimaal 15 minuten")
    @Max(value = 480, message = "Maximaal 8 uur")
    private Integer durationMinutes;

    private Long assignedStaffId;
}
