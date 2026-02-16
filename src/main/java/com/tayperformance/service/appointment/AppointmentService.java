package com.tayperformance.service.appointment;

import com.tayperformance.dto.appointment.AppointmentResponse;
import com.tayperformance.dto.appointment.CreateAppointmentRequest;
import com.tayperformance.dto.appointment.UpdateAppointmentRequest;
import com.tayperformance.entity.*;
import com.tayperformance.exception.BadRequestException;
import com.tayperformance.exception.NotFoundException;
import com.tayperformance.mapper.AppointmentMapper;
import com.tayperformance.repository.AppointmentRepository;
import com.tayperformance.repository.SmsLogRepository;
import com.tayperformance.repository.UserRepository;
import com.tayperformance.service.appointment.core.AppointmentConflictChecker;
import com.tayperformance.service.appointment.core.AppointmentSmsScheduler;
import com.tayperformance.service.appointment.core.AppointmentValidator;
import com.tayperformance.service.customer.CustomerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class AppointmentService {

    private final AppointmentRepository appointmentRepo;
    private final UserRepository userRepo;

    private final AppointmentValidator validator;
    private final AppointmentConflictChecker conflictChecker;
    private final AppointmentSmsScheduler smsScheduler;
    private final CustomerService customerService;
    private final SmsLogRepository smsLogRepo;

    // -------------------------
    // PUBLIC REQUEST (REQUESTED)
    // -------------------------
    public AppointmentResponse createPublicRequest(CreateAppointmentRequest req) {
        log.info("Public request phone={}", req.getCustomerPhone());

        validator.validateStartInFuture(req.getStartTime());

        Customer customer = customerService.findOrCreate(req.getCustomerPhone(), req.getCustomerName());

        int minutes = (req.getDurationMinutes() != null) ? req.getDurationMinutes() : 60;
        validator.validateDuration(minutes);

        OffsetDateTime endTime = req.getStartTime().plusMinutes(minutes);

        Appointment appt = Appointment.builder()
                .customer(customer)
                .carBrand(req.getCarBrand())
                .carModel(req.getCarModel())
                .description(req.getDescription())
                .startTime(req.getStartTime())
                .endTime(endTime)
                .status(AppointmentStatus.REQUESTED)
                .build();

        appt = appointmentRepo.save(appt);

        // (optioneel) geen SMS bij REQUESTED, want nog niet bevestigd
        return AppointmentMapper.toResponse(appt);
    }

    // -------------------------
    // INTERNAL CREATE (CONFIRMED)
    // -------------------------
    public AppointmentResponse createConfirmedAppointment(CreateAppointmentRequest req) {
        log.info("Internal confirmed phone={}", req.getCustomerPhone());

        validator.validateStartInFuture(req.getStartTime());

        if (req.getPrice() == null) throw new BadRequestException("Prijs is verplicht voor directe booking");
        if (req.getDurationMinutes() == null) throw new BadRequestException("Duur is verplicht");

        validator.validateDuration(req.getDurationMinutes());

        Customer customer = customerService.findOrCreate(req.getCustomerPhone(), req.getCustomerName());

        Long staffId = req.getAssignedStaffId();
        if (staffId == null) {
            String username = com.tayperformance.security.SecurityUtils.currentUsername();
            if (username == null) throw new BadRequestException("Staff is verplicht");

            staffId = userRepo.findByUsernameIgnoreCase(username)
                    .orElseThrow(() -> new NotFoundException("Ingelogde gebruiker niet gevonden"))
                    .getId();
        }

        User staff = userRepo.findById(staffId)
                .orElseThrow(() -> new NotFoundException("Staff niet gevonden"));

        OffsetDateTime endTime = req.getStartTime().plusMinutes(req.getDurationMinutes());

        Appointment appt = Appointment.builder()
                .customer(customer)
                .assignedStaff(staff)
                .carBrand(req.getCarBrand())
                .carModel(req.getCarModel())
                .description(req.getDescription())
                .startTime(req.getStartTime())
                .endTime(endTime)
                .price(req.getPrice())
                .status(AppointmentStatus.CONFIRMED)
                .build();

        conflictChecker.ensureNoConflict(appt);

        appt = appointmentRepo.save(appt);

        // ✅ stuur confirm sms na commit
        smsScheduler.schedule(appt, SmsType.CONFIRM);

        return AppointmentMapper.toResponse(appt);
    }

    // -------------------------
    // UPDATE
    // -------------------------
    public AppointmentResponse update(Long id, UpdateAppointmentRequest req) {
        Appointment appt = appointmentRepo.findById(id)
                .orElseThrow(() -> new NotFoundException("Afspraak niet gevonden"));

        validator.ensureModifiable(appt);

        boolean needsConflictCheck = false;
        boolean needsSms = false;

        // We houden durationMinutes bij via endTime - startTime
        // maar in jouw model zet je endTime expliciet.
        // Dus bij startTime en/of duration wijziging => endTime opnieuw berekenen.
        Integer newDuration = null;

        if (req.getDurationMinutes() != null) {
            validator.validateDuration(req.getDurationMinutes());
            newDuration = req.getDurationMinutes();
            needsConflictCheck = true;
            needsSms = true;
        }

        if (req.getStartTime() != null) {
            validator.validateStartInFuture(req.getStartTime());
            appt.setStartTime(req.getStartTime());
            needsConflictCheck = true;
            needsSms = true;
        }

        // ✅ endTime herberekenen als start/duration wijzigde
        if (needsSms) {
            int minutes;
            if (newDuration != null) {
                minutes = newDuration;
            } else {
                // als duration niet meegegeven is, probeer uit bestaande endTime te rekenen
                if (appt.getEndTime() != null) {
                    minutes = (int) java.time.Duration.between(appt.getStartTime(), appt.getEndTime()).toMinutes();
                    if (minutes <= 0) minutes = 60;
                } else {
                    minutes = 60;
                }
            }
            appt.setEndTime(appt.getStartTime().plusMinutes(minutes));
        }

        if (req.getAssignedStaffId() != null) {
            User staff = userRepo.findById(req.getAssignedStaffId())
                    .orElseThrow(() -> new NotFoundException("Staff niet gevonden"));
            appt.setAssignedStaff(staff);
            needsConflictCheck = true;
        }

        if (req.getPrice() != null) appt.setPrice(req.getPrice());
        if (req.getDescription() != null) appt.setDescription(req.getDescription());
        if (req.getCarBrand() != null) appt.setCarBrand(req.getCarBrand());
        if (req.getCarModel() != null) appt.setCarModel(req.getCarModel());

        if (needsConflictCheck) conflictChecker.ensureNoConflict(appt);

        appt = appointmentRepo.save(appt);

        if (needsSms) smsScheduler.schedule(appt, SmsType.UPDATE);

        return AppointmentMapper.toResponse(appt);
    }

    // -------------------------
    // CONFIRM REQUESTED -> CONFIRMED
    // -------------------------
    public AppointmentResponse confirmRequest(Long id, UpdateAppointmentRequest req) {
        Appointment appt = appointmentRepo.findById(id)
                .orElseThrow(() -> new NotFoundException("Afspraak niet gevonden"));

        if (appt.getStatus() != AppointmentStatus.REQUESTED) {
            throw new BadRequestException("Alleen REQUESTED afspraken kunnen bevestigd worden");
        }

        if (req.getAssignedStaffId() == null) throw new BadRequestException("Staff is verplicht");
        if (req.getPrice() == null) throw new BadRequestException("Prijs is verplicht");
        if (req.getDurationMinutes() == null) throw new BadRequestException("Duur is verplicht");

        validator.validateDuration(req.getDurationMinutes());

        User staff = userRepo.findById(req.getAssignedStaffId())
                .orElseThrow(() -> new NotFoundException("Staff niet gevonden"));

        appt.setAssignedStaff(staff);
        appt.setPrice(req.getPrice());
        appt.setEndTime(appt.getStartTime().plusMinutes(req.getDurationMinutes()));
        appt.setStatus(AppointmentStatus.CONFIRMED);

        conflictChecker.ensureNoConflict(appt);

        appt = appointmentRepo.save(appt);
        smsScheduler.schedule(appt, SmsType.CONFIRM);

        return AppointmentMapper.toResponse(appt);
    }

    // -------------------------
    // CANCEL
    // -------------------------
    public AppointmentResponse cancel(Long id, String reason) {
        Appointment appt = appointmentRepo.findById(id)
                .orElseThrow(() -> new NotFoundException("Afspraak niet gevonden"));

        if (appt.getStatus() == AppointmentStatus.CANCELED) {
            throw new BadRequestException("Afspraak is al geannuleerd");
        }
        if (appt.getStatus() == AppointmentStatus.COMPLETED) {
            throw new BadRequestException("Afgeronde afspraken kunnen niet geannuleerd worden");
        }

        appt.setStatus(AppointmentStatus.CANCELED);

        if (reason != null && !reason.isBlank()) {
            String desc = appt.getDescription() == null ? "" : appt.getDescription();
            appt.setDescription(desc + "\nAnnuleringsreden: " + reason.trim());
        }

        appt = appointmentRepo.save(appt);
        smsScheduler.schedule(appt, SmsType.CANCEL);

        return AppointmentMapper.toResponse(appt);
    }

    // -------------------------
    // STATUS transitions
    // -------------------------
    public AppointmentResponse markInProgress(Long id) { return transitionStatus(id, AppointmentStatus.IN_PROGRESS); }
    public AppointmentResponse markCompleted(Long id)  { return transitionStatus(id, AppointmentStatus.COMPLETED); }
    public AppointmentResponse markNoShow(Long id)     { return transitionStatus(id, AppointmentStatus.NOSHOW); }

    private AppointmentResponse transitionStatus(Long id, AppointmentStatus newStatus) {
        Appointment appt = appointmentRepo.findById(id)
                .orElseThrow(() -> new NotFoundException("Afspraak niet gevonden"));

        appt.setStatus(newStatus);
        appt = appointmentRepo.save(appt);

        return AppointmentMapper.toResponse(appt);
    }

    // -------------------------
    // GET / SEARCH
    // -------------------------
    @Transactional(readOnly = true)
    public AppointmentResponse getById(Long id) {
        Appointment appt = appointmentRepo.findById(id)
                .orElseThrow(() -> new NotFoundException("Afspraak niet gevonden"));
        return AppointmentMapper.toResponse(appt);
    }

    @Transactional(readOnly = true)
    public Page<AppointmentResponse> search(String q, java.time.LocalDate date, Pageable pageable) {

        String term = (q == null) ? "" : q.trim();

        // ✅ GEEN date-filter -> gebruik simpele query (geen null OffsetDateTime params)
        if (date == null) {
            // optie A: altijd search()
            Page<Appointment> page = appointmentRepo.search(term, pageable);
            return page.map(AppointmentMapper::toResponse);

            // optie B (sneller): als term leeg is, pak gewoon alles
            // if (term.isBlank()) return appointmentRepo.findAllByOrderByStartTimeDesc(pageable).map(AppointmentMapper::toResponse);
            // return appointmentRepo.search(term, pageable).map(AppointmentMapper::toResponse);
        }

        // ✅ MET date-filter -> gebruik searchAdvanced met echte from/to
        java.time.ZoneId zone = java.time.ZoneId.systemDefault();
        java.time.OffsetDateTime from = date.atStartOfDay(zone).toOffsetDateTime();
        java.time.OffsetDateTime to = from.plusDays(1);

        Page<Appointment> page = appointmentRepo.searchAdvanced(term, from, to, pageable);
        return page.map(AppointmentMapper::toResponse);
    }


    public void delete(Long id) {
        Appointment appt = appointmentRepo.findById(id)
                .orElseThrow(() -> new NotFoundException("Afspraak niet gevonden"));

        smsLogRepo.deleteByAppointment_Id(id); // ✅ eerst logs weg
        appointmentRepo.delete(appt);          // ✅ dan afspraak weg
    }

}
