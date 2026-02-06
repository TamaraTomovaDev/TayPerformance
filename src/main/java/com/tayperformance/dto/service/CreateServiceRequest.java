package com.tayperformance.dto.service;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateServiceRequest {
    @NotBlank
    private String name;

    private String description;

    @NotNull
    private Integer defaultMinutes;

    private Integer minMinutes;
    private Integer maxMinutes;
}
