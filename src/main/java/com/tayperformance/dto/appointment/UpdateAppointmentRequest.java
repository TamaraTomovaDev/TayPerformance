package com.tayperformance.dto.appointment;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class UpdateAppointmentRequest {
    private BigDecimal price;
    private String description;
    private String carBrand;
    private String carModel;
}