package com.tayperformance.dto.appointment;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ConfirmAppointmentRequest {

    @NotNull(message = "Duur is verplicht")
    @Min(value = 15, message = "Duur moet minstens 15 minuten zijn")
    private Integer durationMinutes;

    @DecimalMin(value = "0.0", inclusive = true, message = "Prijs moet >= 0 zijn")
    private BigDecimal price;

    @NotNull(message = "Staff is verplicht")
    private Long assignedStaffId;
}
