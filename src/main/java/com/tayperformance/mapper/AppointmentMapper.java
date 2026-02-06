package com.tayperformance.mapper;

import com.tayperformance.dto.appointment.AppointmentResponse;
import com.tayperformance.entity.Appointment;

public final class AppointmentMapper {

    private AppointmentMapper() {
        throw new AssertionError("Utility class");
    }

    public static AppointmentResponse toResponse(Appointment a) {
        if (a == null) return null;

        return AppointmentResponse.builder()
                .id(a.getId())
                .customerId(a.getCustomer() != null ? a.getCustomer().getId() : null)
                .customerName(a.getCustomer() != null ? a.getCustomer().getDisplayName() : null)
                .customerPhone(a.getCustomer() != null ? a.getCustomer().getPhone() : null)
                .serviceId(a.getService() != null ? a.getService().getId() : null)
                .serviceName(a.getService() != null ? a.getService().getName() : null)
                .assignedStaffId(a.getAssignedStaff() != null ? a.getAssignedStaff().getId() : null)
                .assignedStaffName(a.getAssignedStaff() != null ? a.getAssignedStaff().getDisplayName() : null)
                .carBrand(a.getCarBrand())
                .carModel(a.getCarModel())
                .description(a.getDescription())
                .price(a.getPrice())
                .startTime(a.getStartTime())
                .endTime(a.getEndTime())
                .durationMinutes(a.getDurationMinutes())
                .status(a.getStatus())
                .createdAt(a.getCreatedAt())
                .updatedAt(a.getUpdatedAt())
                .build();
    }
}