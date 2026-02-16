package com.tayperformance.mapper;

import com.tayperformance.dto.appointment.AppointmentResponse;
import com.tayperformance.entity.Appointment;
import com.tayperformance.entity.Customer;

import java.time.Duration;

public final class AppointmentMapper {

    private AppointmentMapper() {
        // utility class
    }

    public static AppointmentResponse toResponse(Appointment a) {

        if (a == null) return null;

        Customer c = a.getCustomer();

        Integer duration = null;
        if (a.getStartTime() != null && a.getEndTime() != null) {
            duration = (int) Duration.between(a.getStartTime(), a.getEndTime()).toMinutes();
        }

        return AppointmentResponse.builder()
                .id(a.getId())

                .customerId(c != null ? c.getId() : null)
                .customerPhone(c != null ? c.getPhone() : null)
                .customerName(c != null ? c.getDisplayName() : null)

                .assignedStaffId(
                        a.getAssignedStaff() != null
                                ? a.getAssignedStaff().getId()
                                : null
                )

                .carBrand(a.getCarBrand())
                .carModel(a.getCarModel())
                .description(a.getDescription())

                .startTime(a.getStartTime())
                .endTime(a.getEndTime())

                .durationMinutes(duration)

                .price(a.getPrice())
                .status(a.getStatus())

                .build();
    }
}
