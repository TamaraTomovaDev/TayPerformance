// ============================================================
// REQUEST DTOs
// ============================================================

package com.tayperformance.dto.appointment;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * Request DTO voor het aanmaken van afspraken.
 * Gebruikt door publieke website (REQUESTED) EN interne app (CONFIRMED).
 */
@Data
public class CreateAppointmentRequest {

    // Verplicht voor beide flows
    @NotBlank(message = "Telefoonnummer is verplicht")
    private String customerPhone;

    @NotBlank(message = "Automerk is verplicht")
    private String carBrand;

    @NotBlank(message = "Omschrijving is verplicht")
    private String description;

    @NotNull(message = "Starttijd is verplicht")
    @Future(message = "Starttijd moet in de toekomst liggen")
    private OffsetDateTime startTime;

    // Optioneel (context afhankelijk)
    private String customerName;
    private String carModel;
    private Long serviceId;

    @Positive(message = "Prijs moet positief zijn")
    private BigDecimal price;

    @Min(value = 15, message = "Minimaal 15 minuten")
    @Max(value = 480, message = "Maximaal 8 uur")
    private Integer durationMinutes;

    private Long assignedStaffId;
}