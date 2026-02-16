package com.tayperformance.dto.appointment;

import com.tayperformance.entity.AppointmentStatus;
import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Value
@Builder
public class AppointmentResponse {

    Long id;

    Long customerId;
    String customerPhone;
    String customerName;

    Long assignedStaffId;

    String carBrand;
    String carModel;
    String description;

    OffsetDateTime startTime;
    OffsetDateTime endTime;

    Integer durationMinutes;

    BigDecimal price;

    AppointmentStatus status;
}
