package com.tayperformance.service.appointment;

import com.tayperformance.dto.appointment.AppointmentResponse;
import com.tayperformance.entity.Appointment;

public final class AppointmentMapper {

    private AppointmentMapper() {
        // utility class
    }

    public static AppointmentResponse toResponse(Appointment appointment) {
        return AppointmentResponse.builder()
                .id(appointment.getId())
                .customerName(appointment.getCustomer().getFirstName())
                .customerPhone(appointment.getCustomer().getPhone())
                .carBrand(appointment.getCarBrand())
                .carModel(appointment.getCarModel())
                .description(appointment.getDescription())
                .price(appointment.getPrice())
                .startTime(appointment.getStartTime())
                .endTime(appointment.getEndTime())
                .status(appointment.getStatus())
                .createdAt(appointment.getCreatedAt())
                .build();
    }
}