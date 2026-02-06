package com.tayperformance.service.appointment.core;

import com.tayperformance.entity.Appointment;
import com.tayperformance.exception.BadRequestException;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;

@Service
public class AppointmentValidator {

    public void validateStartInFuture(OffsetDateTime start) {
        if (start == null)
            throw new BadRequestException("Starttijd is verplicht");

        if (start.isBefore(OffsetDateTime.now()))
            throw new BadRequestException("Afspraak moet in de toekomst liggen");
    }

    public void validateDuration(Integer minutes) {
        if (minutes == null || minutes <= 0)
            throw new BadRequestException("Duur moet groter dan 0 zijn");

        if (minutes > 480)
            throw new BadRequestException("Duur mag niet langer dan 8 uur zijn");
    }

    public void ensureModifiable(Appointment appt) {
        if (!appt.isModifiable()) {
            throw new BadRequestException("Afspraak kan niet meer gewijzigd worden (status: " + appt.getStatus() + ")");
        }
    }
}