package com.tayperformance.controller.internalapi;

import com.tayperformance.dto.sms.SmsLogResponse;
import com.tayperformance.entity.SmsLog;
import com.tayperformance.entity.SmsStatus;
import com.tayperformance.exception.BadRequestException;
import com.tayperformance.exception.NotFoundException;
import com.tayperformance.mapper.SmsLogMapper;
import com.tayperformance.repository.AppointmentRepository;
import com.tayperformance.repository.SmsLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/internal/sms-logs")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','STAFF')")
public class InternalSmsLogController {

    private final SmsLogRepository smsLogRepo;
    private final AppointmentRepository appointmentRepo;

    // ------------------------------------------------------------
    // 1) Logs per afspraak
    // GET /api/internal/sms-logs/appointments/{appointmentId}
    // ------------------------------------------------------------
    @GetMapping("/appointments/{appointmentId}")
    public List<SmsLogResponse> listByAppointment(@PathVariable Long appointmentId) {

        if (!appointmentRepo.existsById(appointmentId)) {
            throw NotFoundException.of("Appointment", appointmentId);
        }

        return smsLogRepo.findAllByAppointment_IdOrderByCreatedAtDesc(appointmentId)
                .stream()
                .map(SmsLogMapper::toResponse)
                .toList();
    }

    // ------------------------------------------------------------
    // 2) 1 log ophalen
    // GET /api/internal/sms-logs/{id}
    // ------------------------------------------------------------
    @GetMapping("/{id}")
    public SmsLogResponse getById(@PathVariable Long id) {
        SmsLog log = smsLogRepo.findById(id)
                .orElseThrow(() -> new NotFoundException("SMS log niet gevonden"));
        return SmsLogMapper.toResponse(log);
    }

    // ------------------------------------------------------------
    // 3) Failed logs (paginatie)
    // GET /api/internal/sms-logs/failed?sinceDays=7
    // ------------------------------------------------------------
    @GetMapping("/failed")
    public Page<SmsLogResponse> failed(
            @RequestParam(defaultValue = "7") int sinceDays,
            Pageable pageable
    ) {
        if (sinceDays < 1 || sinceDays > 90) {
            throw new BadRequestException("sinceDays moet tussen 1 en 90 zijn");
        }

        OffsetDateTime since = OffsetDateTime.now().minusDays(sinceDays);

        return smsLogRepo.findFailedSince(since, pageable)
                .map(SmsLogMapper::toResponse);
    }

    // ------------------------------------------------------------
    // 4) Filter op status (paginatie)
    // GET /api/internal/sms-logs?status=SENT
    // ------------------------------------------------------------
    @GetMapping
    public Page<SmsLogResponse> list(
            @RequestParam(required = false) SmsStatus status,
            Pageable pageable
    ) {
        if (status == null) {
            return smsLogRepo.findAll(pageable).map(SmsLogMapper::toResponse);
        }
        return smsLogRepo.findAllByStatusOrderByCreatedAtDesc(status, pageable)
                .map(SmsLogMapper::toResponse);
    }
}
