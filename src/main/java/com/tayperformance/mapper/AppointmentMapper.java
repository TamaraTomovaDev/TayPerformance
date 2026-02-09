package com.tayperformance.mapper;

import com.tayperformance.dto.appointment.AppointmentResponse;
import com.tayperformance.entity.Appointment;
import com.tayperformance.entity.Customer;

public class AppointmentMapper {

    public static AppointmentResponse toResponse(Appointment a) {
        Customer c = a.getCustomer();

        return AppointmentResponse.builder()
                .id(a.getId())
                .customerId(c != null ? c.getId() : null)
                .customerPhone(c != null ? c.getPhone() : null)
                .customerName(c != null ? c.getDisplayName() : null)
                .assignedStaffId(a.getAssignedStaff() != null ? a.getAssignedStaff().getId() : null)
                .carBrand(a.getCarBrand())
                .carModel(a.getCarModel())
                .description(a.getDescription())
                .startTime(a.getStartTime())
                .endTime(a.getEndTime())
                .durationMinutes(a.getDurationMinutes())
                .price(a.getPrice())
                .status(a.getStatus())
                .build();
    }
}
