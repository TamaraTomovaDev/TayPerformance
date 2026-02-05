package com.tayperformance.dto.appointment;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class ConfirmAppointmentRequest {

    @NotNull(message = "Duur is verplicht")
    @Min(15)
    private Integer durationMinutes;

    private BigDecimal price;

    @NotNull(message = "Staff is verplicht")
    private Long assignedStaffId;
}