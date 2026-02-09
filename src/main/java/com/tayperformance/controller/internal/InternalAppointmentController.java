package com.tayperformance.controller.internal;

import com.tayperformance.dto.appointment.AppointmentResponse;
import com.tayperformance.dto.appointment.CreateAppointmentRequest;
import com.tayperformance.dto.appointment.UpdateAppointmentRequest;
import com.tayperformance.service.appointment.AppointmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/internal/appointments")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','STAFF')")
public class InternalAppointmentController {

    private final AppointmentService service;

    /** Interne app: direct bevestigde afspraak (CONFIRMED). */
    @PostMapping
    public AppointmentResponse createConfirmed(@Valid @RequestBody CreateAppointmentRequest req) {
        return service.createConfirmedAppointment(req);
    }

    /** Partial update (PATCH). */
    @PatchMapping("/{id}")
    public AppointmentResponse update(@PathVariable Long id, @Valid @RequestBody UpdateAppointmentRequest req) {
        return service.update(id, req);
    }

    /** REQUESTED -> CONFIRMED. */
    @PostMapping("/{id}/confirm")
    public AppointmentResponse confirm(@PathVariable Long id, @Valid @RequestBody UpdateAppointmentRequest req) {
        return service.confirmRequest(id, req);
    }

    /** Cancel (optionele reason). */
    @PostMapping("/{id}/cancel")
    public AppointmentResponse cancel(@PathVariable Long id, @RequestParam(required = false) String reason) {
        return service.cancel(id, reason);
    }

    /** Status transitions. */
    @PostMapping("/{id}/start")
    public AppointmentResponse start(@PathVariable Long id) {
        return service.markInProgress(id);
    }

    @PostMapping("/{id}/complete")
    public AppointmentResponse complete(@PathVariable Long id) {
        return service.markCompleted(id);
    }

    @PostMapping("/{id}/noshow")
    public AppointmentResponse noShow(@PathVariable Long id) {
        return service.markNoShow(id);
    }

    /** Read. */
    @GetMapping("/{id}")
    public AppointmentResponse get(@PathVariable Long id) {
        return service.getById(id);
    }

    /** Search + pagination. */
    @GetMapping
    public Page<?> search(@RequestParam(required = false) String q, Pageable pageable) {
        // Als je wil: maak dit Page<AppointmentResponse> (zie note hieronder)
        return service.search(q, pageable);
    }
}
