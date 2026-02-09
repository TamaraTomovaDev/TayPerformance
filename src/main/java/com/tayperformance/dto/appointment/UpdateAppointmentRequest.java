package com.tayperformance.dto.appointment;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Data
public class UpdateAppointmentRequest {

    @Future(message = "Starttijd moet in de toekomst liggen")
    private OffsetDateTime startTime;

    @Min(15) @Max(480)
    private Integer durationMinutes;

    private Long assignedStaffId;

    @Positive(message = "Prijs moet positief zijn")
    private BigDecimal price;

    private String description;
    private String carBrand;
    private String carModel;
}
