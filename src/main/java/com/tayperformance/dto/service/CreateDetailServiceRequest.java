package com.tayperformance.dto.service;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CreateDetailServiceRequest {

    @NotBlank
    private String name;

    @NotNull
    @Min(15)
    @Max(480)
    private Integer defaultMinutes;

    @DecimalMin("0.00")
    private BigDecimal basePrice;
}
