package com.tayperformance.dto.appointment;

import com.tayperformance.entity.AppointmentStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Data
@Builder
public class AppointmentResponse {

    private Long id;

    // Customer
    private String customerName;
    private String customerPhone;

    // Car
    private String carBrand;
    private String carModel;

    // Appointment
    private String description;
    private BigDecimal price;
    private OffsetDateTime startTime;
    private OffsetDateTime endTime;
    private AppointmentStatus status;

    // Audit
    private OffsetDateTime createdAt;
}
