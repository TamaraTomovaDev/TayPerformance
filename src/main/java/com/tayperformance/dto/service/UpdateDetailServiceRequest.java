package com.tayperformance.dto.service;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class UpdateDetailServiceRequest {
    private String name;
    private Integer defaultMinutes;
    private BigDecimal basePrice;
    private Boolean active;
}
