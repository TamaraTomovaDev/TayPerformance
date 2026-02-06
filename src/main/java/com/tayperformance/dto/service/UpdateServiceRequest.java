package com.tayperformance.dto.service;

import lombok.Data;

@Data
public class UpdateServiceRequest {
    private String name;
    private String description;
    private Integer defaultMinutes;
    private Integer minMinutes;
    private Integer maxMinutes;
    private Boolean active;
}

