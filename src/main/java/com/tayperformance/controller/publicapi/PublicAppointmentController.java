package com.tayperformance.controller.publicapi;

import com.tayperformance.dto.appointment.AppointmentResponse;
import com.tayperformance.dto.appointment.CreateAppointmentRequest;
import com.tayperformance.service.appointment.AppointmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/public/appointments")
@RequiredArgsConstructor
public class PublicAppointmentController {

    private final AppointmentService service;

    /**
     * Publieke website: klant vraagt afspraak aan (REQUESTED).
     */
    @PostMapping
    public AppointmentResponse create(@Valid @RequestBody CreateAppointmentRequest req) {
        return service.createPublicRequest(req);
    }

    /**
     * Klant kan eigen afspraak bekijken (optioneel).
     */
    @GetMapping("/{id}")
    public AppointmentResponse get(@PathVariable Long id) {
        return service.getById(id);
    }
}