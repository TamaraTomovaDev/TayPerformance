package com.tayperformance.dto.appointment;

import com.tayperformance.entity.AppointmentStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * Response DTO voor alle appointment endpoints.
 */
@Data
@Builder
public class AppointmentResponse {
    private Long id;
    private Long customerId;
    private String customerName;
    private String customerPhone;
    private Long serviceId;
    private String serviceName;
    private Long assignedStaffId;
    private String assignedStaffName;
    private String carBrand;
    private String carModel;
    private String description;
    private BigDecimal price;
    private OffsetDateTime startTime;
    private OffsetDateTime endTime;
    private Integer durationMinutes;
    private AppointmentStatus status;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}