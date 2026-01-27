package com.tayperformance.controller.publicapi;

import com.tayperformance.dto.appointment.AppointmentResponse;
import com.tayperformance.dto.appointment.CreateRequestedAppointmentRequest;
import com.tayperformance.service.appointment.AppointmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/public/appointments")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class PublicAppointmentController {

    private final AppointmentService appointmentService;

    @PostMapping
    public ResponseEntity<AppointmentResponse> createRequested(@Valid @RequestBody CreateRequestedAppointmentRequest req) {
        AppointmentResponse created = appointmentService.createRequested(req);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }
}
