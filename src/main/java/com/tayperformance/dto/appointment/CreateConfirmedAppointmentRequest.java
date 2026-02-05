package com.tayperformance.dto.appointment;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Data
public class CreateConfirmedAppointmentRequest {

    @NotBlank(message = "Telefoonnummer is verplicht")
    private String customerPhone;

    private String customerName;

    private Long serviceId; // optioneel

    @NotNull(message = "Staff is verplicht")
    private Long assignedStaffId;

    @NotBlank(message = "Automerk is verplicht")
    private String carBrand;

    private String carModel;

    private String description;

    @NotNull(message = "Starttijd is verplicht")
    @Future(message = "Afspraak moet in de toekomst liggen")
    private OffsetDateTime startTime;

    @NotNull(message = "Duur is verplicht")
    @Min(15)
    @Max(480)
    private Integer durationMinutes;

    private BigDecimal price;
}