package com.tayperformance.dto.service;

import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
@Builder
public class ServiceDto {
    private Long id;
    private String name;
    private String description;
    private Integer minMinutes;
    private Integer maxMinutes;
    private Integer defaultMinutes;
    private boolean active;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}