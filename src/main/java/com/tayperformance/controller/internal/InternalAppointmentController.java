package com.tayperformance.controller.internal;

import com.tayperformance.dto.appointment.AppointmentResponse;
import com.tayperformance.dto.appointment.CreateAppointmentRequest;
import com.tayperformance.dto.appointment.UpdateAppointmentRequest;
import com.tayperformance.service.appointment.AppointmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/internal/appointments")
@RequiredArgsConstructor
public class InternalAppointmentController {

    private final AppointmentService service;

    /**
     * Interne app: direct bevestigde afspraak (CONFIRMED).
     */
    @PostMapping
    public AppointmentResponse createConfirmed(@Valid @RequestBody CreateAppointmentRequest req) {
        return service.createConfirmedAppointment(req);
    }

    /**
     * Appointment gedeeltelijk updaten.
     */
    @PatchMapping("/{id}")
    public AppointmentResponse update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateAppointmentRequest req
    ) {
        return service.update(id, req);
    }

    /**
     * REQUESTED afspraak bevestigen.
     */
    @PostMapping("/{id}/confirm")
    public AppointmentResponse confirm(
            @PathVariable Long id,
            @Valid @RequestBody UpdateAppointmentRequest req
    ) {
        return service.confirmRequest(id, req);
    }

    /**
     * Annuleren.
     */
    @PostMapping("/{id}/cancel")
    public AppointmentResponse cancel(
            @PathVariable Long id,
            @RequestParam(required = false) String reason
    ) {
        return service.cancel(id, reason);
    }

    /**
     * Status naar IN_PROGRESS.
     */
    @PostMapping("/{id}/start")
    public AppointmentResponse start(@PathVariable Long id) {
        return service.markInProgress(id);
    }

    /**
     * Status naar COMPLETED.
     */
    @PostMapping("/{id}/complete")
    public AppointmentResponse complete(@PathVariable Long id) {
        return service.markCompleted(id);
    }

    /**
     * Status naar NOSHOW.
     */
    @PostMapping("/{id}/noshow")
    public AppointmentResponse noShow(@PathVariable Long id) {
        return service.markNoShow(id);
    }

    /**
     * Één afspraak ophalen.
     */
    @GetMapping("/{id}")
    public AppointmentResponse get(@PathVariable Long id) {
        return service.getById(id);
    }

    /**
     * Zoeken + paginatie.
     */
    @GetMapping
    public Page<?> search(
            @RequestParam(required = false) String q,
            Pageable pageable
    ) {
        return service.search(q, pageable);
    }
}