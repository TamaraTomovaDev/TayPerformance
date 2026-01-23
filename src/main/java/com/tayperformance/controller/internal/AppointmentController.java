package com.tayperformance.controller.internal;

import com.tayperformance.dto.appointment.AppointmentResponse;
import com.tayperformance.mapper.AppointmentMapper;
import com.tayperformance.service.appointment.AppointmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Controller voor intern gebruik in de garage (Staff/Admin).
 * Bevat functies voor het overzicht en annuleren van afspraken.
 */
@RestController
@RequestMapping("/api/internal/appointments")
@RequiredArgsConstructor
public class AppointmentController {

    private final AppointmentService appointmentService;

    // Haal alle afspraken op tussen twee datums (voor de kalenderweergave)
    @GetMapping
    public ResponseEntity<List<AppointmentResponse>> getCalendar(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime end
    ) {
        List<AppointmentResponse> responses =
                appointmentService.getAppointmentsBetween(start, end)
                        .stream()
                        .map(AppointmentMapper::toResponse)
                        .toList();

        return ResponseEntity.ok(responses);
    }

    // Annuleer een afspraak (stuurt ook automatisch een annulatie-SMS)
    @DeleteMapping("/{id}/cancel")
    public ResponseEntity<Void> cancelAppointment(@PathVariable Long id) {
        appointmentService.cancelAppointment(id);
        return ResponseEntity.noContent().build();
    }
}
