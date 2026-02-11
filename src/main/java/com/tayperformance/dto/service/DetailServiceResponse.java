package com.tayperformance.dto.service;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class DetailServiceResponse {
    private Long id;
    private String name;
    private Integer defaultMinutes;
    private BigDecimal basePrice;
    private boolean active;
}
