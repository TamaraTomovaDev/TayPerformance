package com.tayperformance.controller.internal;

import com.tayperformance.dto.appointment.AppointmentResponse;
import com.tayperformance.dto.appointment.ConfirmAppointmentRequest;
import com.tayperformance.entity.Appointment;
import com.tayperformance.mapper.AppointmentMapper;
import com.tayperformance.service.appointment.AppointmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.List;

@RestController("internalAppointmentController")
@RequestMapping("/api/internal/appointments")
@RequiredArgsConstructor
public class AppointmentController {

    private final AppointmentService appointmentService;

    @GetMapping
    public ResponseEntity<List<AppointmentResponse>> getCalendar(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime end
    ) {
        List<Appointment> appointments = appointmentService.getBetween(start, end);
        List<AppointmentResponse> responses = appointments.stream()
                .map(AppointmentMapper::toResponse)
                .toList();

        return ResponseEntity.ok(responses);
    }

    @PostMapping("/{id}/confirm")
    public ResponseEntity<AppointmentResponse> confirm(
            @PathVariable Long id,
            @Valid @RequestBody ConfirmAppointmentRequest req
    ) {
        return ResponseEntity.ok(appointmentService.confirm(id, req));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<Void> cancel(@PathVariable Long id) {
        appointmentService.cancel(id);
        return ResponseEntity.noContent().build();
    }
}
