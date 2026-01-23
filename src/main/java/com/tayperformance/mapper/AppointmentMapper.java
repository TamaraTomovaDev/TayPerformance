package com.tayperformance.mapper;

import com.tayperformance.dto.appointment.AppointmentResponse;
import com.tayperformance.entity.Appointment;

public class AppointmentMapper {

    private AppointmentMapper() {
        // voorkomt instantiatie
    }

    public static AppointmentResponse toResponse(Appointment appointment) {
        if (appointment == null) {
            return null;
        }

        return AppointmentResponse.builder()
                .id(appointment.getId())

                // Customer
                .customerName(
                        appointment.getCustomer() != null
                                ? appointment.getCustomer().getFirstName()
                                : null
                )
                .customerPhone(
                        appointment.getCustomer() != null
                                ? appointment.getCustomer().getPhone()
                                : null
                )

                // Car
                .carBrand(appointment.getCarBrand())
                .carModel(appointment.getCarModel())

                // Appointment
                .description(appointment.getDescription())
                .price(appointment.getPrice())
                .startTime(appointment.getStartTime())
                .endTime(appointment.getEndTime())
                .status(appointment.getStatus())

                // Audit
                .createdAt(appointment.getCreatedAt())

                .build();
    }
}
