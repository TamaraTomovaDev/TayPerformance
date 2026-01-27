package com.tayperformance.service.appointment;

import com.tayperformance.dto.appointment.AppointmentResponse;
import com.tayperformance.dto.appointment.ConfirmAppointmentRequest;
import com.tayperformance.dto.appointment.CreateRequestedAppointmentRequest;
import com.tayperformance.entity.*;
import com.tayperformance.exception.BadRequestException;
import com.tayperformance.exception.ConflictException;
import com.tayperformance.exception.NotFoundException;
import com.tayperformance.mapper.AppointmentMapper;
import com.tayperformance.repository.*;
import com.tayperformance.service.sms.SmsService;
import com.tayperformance.util.PhoneNumberHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.OffsetDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AppointmentService {

    private static final List<AppointmentStatus> BLOCKING_STATUSES =
            List.of(AppointmentStatus.CONFIRMED, AppointmentStatus.RESCHEDULED);

    private final AppointmentRepository appointmentRepository;
    private final CustomerRepository customerRepository;
    private final ServiceRepository serviceRepository;
    private final UserRepository userRepository;
    private final SmsService smsService;

    @Value("${tay.phone.default-country:BE}")
    private String defaultCountry;

    /**
     * Website/klant: maakt een REQUESTED afspraak aan.
     * - startTime verplicht
     * - service verplicht
     * - endTime + duration = service.defaultMinutes (placeholder)
     * - price blijft null
     */
    @Transactional
    public AppointmentResponse createRequested(CreateRequestedAppointmentRequest req) {
        // 1) Validate input (fail fast)
        if (req.getStartTime() == null) {
            throw new BadRequestException("Starttijd is verplicht.");
        }
        OffsetDateTime now = OffsetDateTime.now();
        if (req.getStartTime().isBefore(now)) {
            throw new BadRequestException("Afspraak moet in de toekomst liggen.");
        }

        String phone = normalizeOrThrow(req.getCustomerPhone());

        // 2) Load dependencies
        DetailService service = serviceRepository.findById(req.getServiceId())
                .orElseThrow(() -> NotFoundException.of("Service", req.getServiceId()));
        validateServiceDuration(service);

        Customer customer = upsertCustomer(phone, req.getCustomerName());

        // 3) Build appointment (REQUESTED)
        int duration = service.getDefaultMinutes();
        OffsetDateTime start = req.getStartTime();
        OffsetDateTime end = start.plusMinutes(duration);

        Appointment appointment = Appointment.builder()
                .customer(customer)
                .service(service)
                .carBrand(requiredText(req.getCarBrand(), "Automerk is verplicht."))
                .carModel(req.getCarModel())
                .description(req.getDescription())
                .startTime(start)
                .durationMinutes(duration)
                .endTime(end)
                .price(null)
                .status(AppointmentStatus.REQUESTED)
                .build();

        // Soft conflict check is optioneel hier.
        // REQUESTED is "aanvraag" en assignedStaff is meestal null, dus conflict check kan te streng zijn.
        // Als jij wil dat klanten enkel echt vrije starturen kunnen kiezen: doe hier een GLOBAL check.
        // softCheckConflictsGlobal(appointment);

        // 4) Save
        Appointment saved = appointmentRepository.saveAndFlush(appointment);

        log.info("Created REQUESTED appointment id={} start={} customer={}",
                saved.getId(), saved.getStartTime(), saved.getCustomer().getPhone());

        return AppointmentMapper.toResponse(saved);
    }

    /**
     * Staff/Admin: bevestigt afspraak.
     * - staff verplicht
     * - durationMinutes verplicht
     * - endTime herberekenen
     * - status -> CONFIRMED
     * - Postgres EXCLUDE constraint blokkeert overlap hard
     * - SMS confirm na commit
     */
    @Transactional
    public AppointmentResponse confirm(Long appointmentId, ConfirmAppointmentRequest req) {
        Appointment appt = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> NotFoundException.of("Appointment", appointmentId));

        if (appt.getStatus() != AppointmentStatus.REQUESTED &&
                appt.getStatus() != AppointmentStatus.RESCHEDULED) {
            throw new BadRequestException("Alleen REQUESTED of RESCHEDULED afspraken kunnen bevestigd worden.");
        }

        Integer duration = req.getDurationMinutes();
        if (duration == null || duration <= 0) {
            throw new BadRequestException("durationMinutes moet > 0 zijn.");
        }

        Long staffId = req.getAssignedStaffId();
        if (staffId == null) {
            throw new BadRequestException("assignedStaffId is verplicht.");
        }

        User staff = userRepository.findById(staffId)
                .orElseThrow(() -> NotFoundException.of("User", staffId));

        // Apply changes
        appt.setAssignedStaff(staff);
        appt.setDurationMinutes(duration);
        appt.setEndTime(appt.getStartTime().plusMinutes(duration));
        appt.setPrice(req.getPrice());
        appt.setStatus(AppointmentStatus.CONFIRMED);

        // Soft conflict check per staff (vriendelijke fout vóór DB)
        // Niet strikt nodig, DB is guard, maar geeft betere message.
        softCheckConflictsForStaff(appt);

        try {
            Appointment saved = appointmentRepository.saveAndFlush(appt);

            registerSmsAfterCommit(saved, SmsType.CONFIRM);

            log.info("Confirmed appointment id={} staff={} start={} end={}",
                    saved.getId(), staff.getId(), saved.getStartTime(), saved.getEndTime());

            return AppointmentMapper.toResponse(saved);
        } catch (DataIntegrityViolationException ex) {
            // DB overlap guard (EXCLUDE constraint) -> 409 via GlobalExceptionHandler
            log.warn("DB conflict confirming appointment id={} (likely overlap): {}",
                    appointmentId, rootMessage(ex));
            throw ex;
        }
    }

    /**
     * Staff/Admin: annuleert afspraak.
     * Idempotent: als al CANCELED -> no-op.
     */
    @Transactional
    public void cancel(Long id) {
        Appointment appt = appointmentRepository.findById(id)
                .orElseThrow(() -> NotFoundException.of("Appointment", id));

        if (appt.getStatus() == AppointmentStatus.COMPLETED) {
            throw new BadRequestException("Een afgewerkte afspraak kan niet geannuleerd worden.");
        }
        if (appt.getStatus() == AppointmentStatus.CANCELED) {
            return; // idempotent
        }

        appt.setStatus(AppointmentStatus.CANCELED);
        appointmentRepository.saveAndFlush(appt);

        registerSmsAfterCommit(appt, SmsType.CANCEL);

        log.info("Canceled appointment id={} start={} customer={}",
                appt.getId(), appt.getStartTime(), appt.getCustomer().getPhone());
    }

    // ---------------- Helpers ----------------

    private void validateServiceDuration(DetailService service) {
        Integer def = service.getDefaultMinutes();
        if (def == null || def <= 0) {
            throw new BadRequestException("Service configuratie is ongeldig (defaultMinutes).");
        }
        if (service.getMinMinutes() != null && def < service.getMinMinutes()) {
            throw new BadRequestException("Service defaultMinutes is lager dan minMinutes.");
        }
        if (service.getMaxMinutes() != null && def > service.getMaxMinutes()) {
            throw new BadRequestException("Service defaultMinutes is hoger dan maxMinutes.");
        }
    }

    private void softCheckConflictsForStaff(Appointment appt) {
        if (appt.getAssignedStaff() == null) return;

        List<Appointment> conflicts = appointmentRepository.findConflicting(
                BLOCKING_STATUSES,
                appt.getAssignedStaff().getId(),
                appt.getStartTime(),
                appt.getEndTime()
        );

        // Exclude self (bij updates)
        conflicts.removeIf(a -> a.getId().equals(appt.getId()));

        if (!conflicts.isEmpty()) {
            Appointment c = conflicts.get(0);
            throw ConflictException.appointmentOverlap(c.getId(), c.getStartTime(), c.getCarBrand());
        }
    }

    @SuppressWarnings("unused")
    private void softCheckConflictsGlobal(Appointment appt) {
        List<Appointment> conflicts = appointmentRepository.findConflicting(
                BLOCKING_STATUSES,
                null,
                appt.getStartTime(),
                appt.getEndTime()
        );
        if (!conflicts.isEmpty()) {
            Appointment c = conflicts.get(0);
            throw ConflictException.appointmentOverlap(c.getId(), c.getStartTime(), c.getCarBrand());
        }
    }

    private String normalizeOrThrow(String phone) {
        String normalized = PhoneNumberHelper.normalize(phone, defaultCountry);
        if (normalized == null) throw new BadRequestException("Ongeldig telefoonnummer.");
        return normalized;
    }

    private Customer upsertCustomer(String phone, String name) {
        return customerRepository.findByPhoneAndActiveTrue(phone)
                .map(c -> {
                    if (name != null && !name.isBlank()
                            && (c.getFirstName() == null || !name.equals(c.getFirstName()))) {
                        c.setFirstName(name);
                    }
                    return c;
                })
                .orElseGet(() -> customerRepository.save(
                        Customer.builder()
                                .phone(phone)
                                .firstName((name != null && !name.isBlank()) ? name : null)
                                .active(true)
                                .build()
                ));
    }

    private void registerSmsAfterCommit(Appointment app, SmsType type) {
        if (!TransactionSynchronizationManager.isActualTransactionActive()) return;

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    switch (type) {
                        case CONFIRM -> smsService.sendConfirmation(app);
                        case CANCEL -> smsService.sendCancellation(app);
                        default -> { }
                    }
                } catch (Exception e) {
                    // SMS failures should not break the original transaction
                    log.error("SMS send failed type={} appointmentId={}", type, app.getId(), e);
                }
            }
        });
    }

    private String requiredText(String value, String message) {
        if (value == null || value.isBlank()) throw new BadRequestException(message);
        return value.trim();
    }

    private String rootMessage(Throwable t) {
        Throwable cur = t;
        while (cur.getCause() != null) cur = cur.getCause();
        return cur.getMessage() != null ? cur.getMessage() : t.toString();
    }

    @Transactional(readOnly = true)
    public List<Appointment> getBetween(OffsetDateTime start, OffsetDateTime end) {
        return appointmentRepository.findAllByStartTimeBetweenOrderByStartTimeAsc(start, end);
    }

}
