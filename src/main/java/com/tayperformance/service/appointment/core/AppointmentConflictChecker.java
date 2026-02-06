package com.tayperformance.service.appointment.core;

import com.tayperformance.entity.Appointment;
import com.tayperformance.entity.AppointmentStatus;
import com.tayperformance.exception.ConflictException;
import com.tayperformance.repository.AppointmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AppointmentConflictChecker {

    private static final List<AppointmentStatus> BLOCKING_STATUSES =
            List.of(AppointmentStatus.CONFIRMED, AppointmentStatus.IN_PROGRESS);

    private final AppointmentRepository repo;

    public void ensureNoConflict(Appointment appt) {
        if (appt.getAssignedStaff() == null) return;

        List<Appointment> conflicts = repo.findConflicting(
                BLOCKING_STATUSES,
                appt.getAssignedStaff().getId(),
                appt.getStartTime(),
                appt.getEndTime(),
                appt.getId()
        );

        conflicts.stream().findFirst().ifPresent(c -> {
            throw ConflictException.appointmentOverlap(
                    c.getId(),
                    c.getStartTime(),
                    c.getCarBrand()
            );
        });
    }
}