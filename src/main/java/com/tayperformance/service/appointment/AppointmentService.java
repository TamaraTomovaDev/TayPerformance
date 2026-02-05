package com.tayperformance.service.appointment;

import com.tayperformance.dto.appointment.*;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.List;

/**
 * Service voor afspraak management.
 * Handles: aanvragen, bevestigen, annuleren, verplaatsen, status updates.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AppointmentService {

    private static final List<AppointmentStatus> BLOCKING_STATUSES =
            List.of(AppointmentStatus.CONFIRMED, AppointmentStatus.IN_PROGRESS);

    private final AppointmentRepository appointmentRepository;
    private final CustomerRepository customerRepository;
    private final DetailServiceRepository serviceRepository;
    private final UserRepository userRepository;
    private final SmsService smsService;

    @Value("${tay.phone.default-country:BE}")
    private String defaultCountry;

    // ============================================================
    // CREATE OPERATIONS
    // ============================================================

    /**
     * Website: Klant vraagt afspraak aan (REQUESTED status).
     * - Geen staff assignment
     * - Geen prijs
     * - Default duur van service
     * - Geen bevestiging SMS (wacht op staff confirm)
     */
    @Transactional
    public AppointmentResponse createRequested(CreateRequestedAppointmentRequest req) {
        validateFutureStartTime(req.getStartTime());

        String phone = normalizePhoneOrThrow(req.getCustomerPhone());
        DetailService service = loadServiceOrThrow(req.getServiceId());
        Customer customer = getOrCreateCustomer(phone, req.getCustomerName());

        OffsetDateTime endTime = req.getStartTime().plusMinutes(service.getDefaultMinutes());

        Appointment appointment = Appointment.builder()
                .customer(customer)
                .service(service)
                .carBrand(requireNonBlank(req.getCarBrand(), "Automerk is verplicht"))
                .carModel(req.getCarModel())
                .description(req.getDescription())
                .startTime(req.getStartTime())
                .endTime(endTime)
                .price(null)
                .status(AppointmentStatus.REQUESTED)
                .build();

        Appointment saved = appointmentRepository.saveAndFlush(appointment);

        log.info("Created REQUESTED appointment id={} start={} customer={}",
                saved.getId(), saved.getStartTime(), saved.getCustomer().getPhone());

        return AppointmentMapper.toResponse(saved);
    }

    /**
     * Staff/Admin: Maakt direct een bevestigde afspraak aan.
     * - Staff assignment verplicht
     * - Duur verplicht
     * - Prijs optioneel
     * - Direct CONFIRMED status
     * - SMS bevestiging
     */
    @Transactional
    public AppointmentResponse createConfirmed(CreateConfirmedAppointmentRequest req) {
        validateFutureStartTime(req.getStartTime());
        validateDuration(req.getDurationMinutes());

        String phone = normalizePhoneOrThrow(req.getCustomerPhone());
        Customer customer = getOrCreateCustomer(phone, req.getCustomerName());
        User staff = loadStaffOrThrow(req.getAssignedStaffId());
        DetailService service = req.getServiceId() != null
                ? loadServiceOrThrow(req.getServiceId())
                : null;

        OffsetDateTime endTime = req.getStartTime().plusMinutes(req.getDurationMinutes());

        Appointment appointment = Appointment.builder()
                .customer(customer)
                .service(service)
                .assignedStaff(staff)
                .carBrand(requireNonBlank(req.getCarBrand(), "Automerk is verplicht"))
                .carModel(req.getCarModel())
                .description(req.getDescription())
                .startTime(req.getStartTime())
                .endTime(endTime)
                .price(req.getPrice())
                .status(AppointmentStatus.CONFIRMED)
                .build();

        // Conflict check voordat DB hit
        checkConflictsForStaff(appointment);

        try {
            Appointment saved = appointmentRepository.saveAndFlush(appointment);
            scheduleConfirmationSms(saved);

            log.info("Created CONFIRMED appointment id={} staff={} start={}",
                    saved.getId(), staff.getId(), saved.getStartTime());

            return AppointmentMapper.toResponse(saved);
        } catch (DataIntegrityViolationException ex) {
            log.warn("DB conflict creating appointment: {}", getRootMessage(ex));
            throw ConflictException.appointmentOverlap(null, appointment.getStartTime(), appointment.getCarBrand());        }
    }

    // ============================================================
    // UPDATE OPERATIONS
    // ============================================================

    /**
     * Staff/Admin: Bevestigt een REQUESTED afspraak.
     * - Status -> CONFIRMED
     * - Staff assignment
     * - Duur en prijs instellen
     * - SMS bevestiging
     */
    @Transactional
    public AppointmentResponse confirm(Long appointmentId, ConfirmAppointmentRequest req) {
        Appointment appt = loadAppointmentOrThrow(appointmentId);

        if (appt.getStatus() != AppointmentStatus.REQUESTED) {
            throw new BadRequestException(
                    "Alleen REQUESTED afspraken kunnen bevestigd worden (huidige status: " + appt.getStatus() + ")"
            );
        }

        validateDuration(req.getDurationMinutes());
        User staff = loadStaffOrThrow(req.getAssignedStaffId());

        appt.setAssignedStaff(staff);
        appt.setEndTime(appt.getStartTime().plusMinutes(req.getDurationMinutes()));
        appt.setPrice(req.getPrice());
        appt.setStatus(AppointmentStatus.CONFIRMED);

        checkConflictsForStaff(appt);

        try {
            Appointment saved = appointmentRepository.saveAndFlush(appt);
            scheduleConfirmationSms(saved);

            log.info("Confirmed appointment id={} staff={}", saved.getId(), staff.getId());
            return AppointmentMapper.toResponse(saved);
        } catch (DataIntegrityViolationException ex) {
            log.warn("DB conflict creating appointment: {}", getRootMessage(ex));
            throw ConflictException.appointmentOverlap(
                    null,
                    appt.getStartTime(),
                    appt.getCarBrand()
            );
        }
    }

    /**
     * Staff/Admin: Verplaats afspraak naar nieuwe tijd.
     * - Nieuwe tijd verplicht
     * - Conflict check
     * - SMS update
     */
    @Transactional
    public AppointmentResponse reschedule(Long appointmentId, RescheduleAppointmentRequest req) {
        Appointment appt = loadAppointmentOrThrow(appointmentId);

        if (!appt.isModifiable()) {
            throw new BadRequestException("Afspraak kan niet meer verplaatst worden (status: " + appt.getStatus() + ")");
        }

        validateFutureStartTime(req.getNewStartTime());

        // Behoud duur, nieuwe start + end tijd
        int currentDuration = appt.getDurationMinutes() != null ? appt.getDurationMinutes() : 60;
        appt.setStartTime(req.getNewStartTime());
        appt.setEndTime(req.getNewStartTime().plusMinutes(currentDuration));
        appt.setStatus(AppointmentStatus.RESCHEDULED);

        if (appt.getAssignedStaff() != null) {
            checkConflictsForStaff(appt);
        }

        try {
            Appointment saved = appointmentRepository.saveAndFlush(appt);
            scheduleUpdateSms(saved);

            log.info("Rescheduled appointment id={} new start={}", saved.getId(), saved.getStartTime());
            return AppointmentMapper.toResponse(saved);
        } catch (DataIntegrityViolationException ex) {
            throw ConflictException.appointmentOverlap(
                    appt.getId(),
                    appt.getStartTime(),
                    appt.getCarBrand()
            );
        }
    }

    /**
     * Staff/Admin: Update afspraak details.
     * - Prijs, beschrijving, auto info
     * - Geen tijd wijziging (gebruik reschedule)
     */
    @Transactional
    public AppointmentResponse update(Long appointmentId, UpdateAppointmentRequest req) {
        Appointment appt = loadAppointmentOrThrow(appointmentId);

        if (!appt.isModifiable()) {
            throw new BadRequestException("Afspraak kan niet meer gewijzigd worden");
        }

        if (req.getPrice() != null) appt.setPrice(req.getPrice());
        if (req.getDescription() != null) appt.setDescription(req.getDescription());
        if (req.getCarBrand() != null) appt.setCarBrand(req.getCarBrand());
        if (req.getCarModel() != null) appt.setCarModel(req.getCarModel());

        Appointment saved = appointmentRepository.saveAndFlush(appt);

        log.info("Updated appointment id={}", saved.getId());
        return AppointmentMapper.toResponse(saved);
    }

    // ============================================================
    // STATUS CHANGES
    // ============================================================

    /**
     * Staff/Admin: Annuleer afspraak.
     * Idempotent.
     */
    @Transactional
    public void cancel(Long id) {
        Appointment appt = loadAppointmentOrThrow(id);

        if (appt.getStatus() == AppointmentStatus.COMPLETED) {
            throw new BadRequestException("Afgewerkte afspraak kan niet geannuleerd worden");
        }
        if (appt.getStatus() == AppointmentStatus.CANCELED) {
            return; // idempotent
        }

        appt.setStatus(AppointmentStatus.CANCELED);
        appointmentRepository.saveAndFlush(appt);

        scheduleCancellationSms(appt);

        log.info("Canceled appointment id={}", appt.getId());
    }

    /**
     * Staff: Start afspraak (IN_PROGRESS).
     */
    @Transactional
    public AppointmentResponse start(Long id) {
        Appointment appt = loadAppointmentOrThrow(id);

        if (appt.getStatus() != AppointmentStatus.CONFIRMED) {
            throw new BadRequestException("Alleen CONFIRMED afspraken kunnen gestart worden");
        }

        appt.setStatus(AppointmentStatus.IN_PROGRESS);
        Appointment saved = appointmentRepository.saveAndFlush(appt);

        log.info("Started appointment id={}", saved.getId());
        return AppointmentMapper.toResponse(saved);
    }

    /**
     * Staff: Rond afspraak af (COMPLETED).
     */
    @Transactional
    public AppointmentResponse complete(Long id) {
        Appointment appt = loadAppointmentOrThrow(id);

        if (appt.getStatus() != AppointmentStatus.IN_PROGRESS &&
                appt.getStatus() != AppointmentStatus.CONFIRMED) {
            throw new BadRequestException("Alleen IN_PROGRESS of CONFIRMED afspraken kunnen afgerond worden");
        }

        if (appt.getPrice() == null) {
            log.warn("Completing appointment id={} without price", id);
        }

        appt.setStatus(AppointmentStatus.COMPLETED);
        Appointment saved = appointmentRepository.saveAndFlush(appt);

        log.info("Completed appointment id={} price={}", saved.getId(), saved.getPrice());
        return AppointmentMapper.toResponse(saved);
    }

    /**
     * Staff: Markeer als no-show.
     */
    @Transactional
    public AppointmentResponse markNoShow(Long id) {
        Appointment appt = loadAppointmentOrThrow(id);

        if (appt.getStatus() != AppointmentStatus.CONFIRMED) {
            throw new BadRequestException("Alleen CONFIRMED afspraken kunnen als no-show gemarkeerd worden");
        }

        appt.setStatus(AppointmentStatus.NOSHOW);
        Appointment saved = appointmentRepository.saveAndFlush(appt);

        log.info("Marked no-show appointment id={}", saved.getId());
        return AppointmentMapper.toResponse(saved);
    }

    // ============================================================
    // QUERIES
    // ============================================================

    @Transactional(readOnly = true)
    public AppointmentResponse getById(Long id) {
        Appointment appt = loadAppointmentOrThrow(id);
        return AppointmentMapper.toResponse(appt);
    }

    @Transactional(readOnly = true)
    public List<AppointmentResponse> getByDateRange(OffsetDateTime start, OffsetDateTime end) {
        return appointmentRepository.findByDateRange(start, end)
                .stream()
                .map(AppointmentMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AppointmentResponse> getTodaysAppointments() {
        ZonedDateTime now = ZonedDateTime.now();
        OffsetDateTime todayStart = now.toLocalDate().atStartOfDay(now.getZone()).toOffsetDateTime();
        OffsetDateTime tomorrowStart = todayStart.plusDays(1);

        return appointmentRepository.findTodaysAppointments(todayStart, tomorrowStart)
                .stream()
                .map(AppointmentMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AppointmentResponse> getPendingConfirmation() {
        return appointmentRepository.findPendingConfirmation()
                .stream()
                .map(AppointmentMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AppointmentResponse> getByCustomer(Long customerId) {
        return appointmentRepository.findAllByCustomerIdOrderByStartTimeDesc(customerId)
                .stream()
                .map(AppointmentMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public Page<AppointmentResponse> search(String searchTerm, Pageable pageable) {
        return appointmentRepository.searchByCustomer(searchTerm, pageable)
                .map(AppointmentMapper::toResponse);
    }

    // ============================================================
    // VALIDATION HELPERS
    // ============================================================

    private void validateFutureStartTime(OffsetDateTime startTime) {
        if (startTime == null) {
            throw new BadRequestException("Starttijd is verplicht");
        }
        if (startTime.isBefore(OffsetDateTime.now())) {
            throw new BadRequestException("Afspraak moet in de toekomst liggen");
        }
    }

    private void validateDuration(Integer minutes) {
        if (minutes == null || minutes <= 0) {
            throw new BadRequestException("Duur moet groter dan 0 zijn");
        }
        if (minutes > 480) { // 8 uur
            throw new BadRequestException("Duur mag niet langer dan 8 uur zijn");
        }
    }

    private void checkConflictsForStaff(Appointment appt) {
        if (appt.getAssignedStaff() == null) return;

        List<Appointment> conflicts = appointmentRepository.findConflicting(
                BLOCKING_STATUSES,
                appt.getAssignedStaff().getId(),
                appt.getStartTime(),
                appt.getEndTime(),
                appt.getId() // exclude self bij updates
        );

        if (!conflicts.isEmpty()) {
            Appointment conflict = conflicts.get(0);
            throw ConflictException.appointmentOverlap(
                    conflict.getId(),
                    conflict.getStartTime(),
                    conflict.getCarBrand()
            );
        }
    }

    // ============================================================
    // DATA ACCESS HELPERS
    // ============================================================

    private Appointment loadAppointmentOrThrow(Long id) {
        return appointmentRepository.findById(id)
                .orElseThrow(() -> NotFoundException.of("Appointment", id));
    }

    private DetailService loadServiceOrThrow(Long id) {
        return serviceRepository.findById(id)
                .orElseThrow(() -> NotFoundException.of("Service", id));
    }

    private User loadStaffOrThrow(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> NotFoundException.of("User", id));

        if (user.getRole() != Role.STAFF && user.getRole() != Role.ADMIN) {
            throw new BadRequestException("Gebruiker is geen staff member");
        }
        if (!user.isActive()) {
            throw new BadRequestException("Gebruiker is niet actief");
        }

        return user;
    }

    private Customer getOrCreateCustomer(String phone, String name) {
        return customerRepository.findByPhoneAndActiveTrue(phone)
                .map(customer -> {
                    // Update naam als nieuw
                    if (name != null && !name.isBlank() && customer.getFirstName() == null) {
                        customer.setFirstName(name.trim());
                        return customerRepository.save(customer);
                    }
                    return customer;
                })
                .orElseGet(() -> customerRepository.save(
                        Customer.builder()
                                .phone(phone)
                                .firstName(name != null && !name.isBlank() ? name.trim() : null)
                                .active(true)
                                .build()
                ));
    }

    // ============================================================
    // SMS SCHEDULING
    // ============================================================

    private void scheduleConfirmationSms(Appointment appt) {
        scheduleSmsAfterCommit(appt, SmsType.CONFIRM);
    }

    private void scheduleCancellationSms(Appointment appt) {
        scheduleSmsAfterCommit(appt, SmsType.CANCEL);
    }

    private void scheduleUpdateSms(Appointment appt) {
        scheduleSmsAfterCommit(appt, SmsType.UPDATE);
    }

    private void scheduleSmsAfterCommit(Appointment appt, SmsType type) {
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            log.warn("No active transaction, skipping SMS for appointment id={}", appt.getId());
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    switch (type) {
                        case CONFIRM -> smsService.sendConfirmation(appt);
                        case CANCEL -> smsService.sendCancellation(appt);
                        case UPDATE -> smsService.sendUpdate(appt);
                        default -> log.warn("Unknown SMS type: {}", type);
                    }
                } catch (Exception e) {
                    log.error("SMS send failed type={} appointmentId={}", type, appt.getId(), e);
                }
            }
        });
    }

    // ============================================================
    // UTILITY HELPERS
    // ============================================================

    private String normalizePhoneOrThrow(String phone) {
        String normalized = PhoneNumberHelper.normalize(phone, defaultCountry);
        if (normalized == null) {
            throw new BadRequestException("Ongeldig telefoonnummer: " + phone);
        }
        return normalized;
    }

    private String requireNonBlank(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new BadRequestException(message);
        }
        return value.trim();
    }

    private String getRootMessage(Throwable t) {
        Throwable current = t;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        return current.getMessage() != null ? current.getMessage() : t.toString();
    }
}