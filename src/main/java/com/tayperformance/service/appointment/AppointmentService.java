package com.tayperformance.service.appointment;

import com.tayperformance.dto.appointment.AppointmentResponse;
import com.tayperformance.dto.appointment.CreateAppointmentRequest;
import com.tayperformance.dto.appointment.UpdateAppointmentRequest;
import com.tayperformance.entity.*;
import com.tayperformance.exception.BadRequestException;
import com.tayperformance.exception.NotFoundException;
import com.tayperformance.mapper.AppointmentMapper;
import com.tayperformance.repository.AppointmentRepository;
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

    public AppointmentResponse createPublicRequest(CreateAppointmentRequest req) {
        log.info("Public request phone={}", req.getCustomerPhone());

        validator.validateStartInFuture(req.getStartTime());

        Customer customer = customerService.findOrCreate(req.getCustomerPhone(), req.getCustomerName());

        // MVP: duration for public request -> default 60 min if not provided
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
        return AppointmentMapper.toResponse(appt);
    }

    public AppointmentResponse createConfirmedAppointment(CreateAppointmentRequest req) {
        log.info("Internal confirmed phone={}", req.getCustomerPhone());

        validator.validateStartInFuture(req.getStartTime());

        if (req.getPrice() == null) throw new BadRequestException("Prijs is verplicht voor directe booking");
        if (req.getAssignedStaffId() == null) throw new BadRequestException("Staff is verplicht");
        if (req.getDurationMinutes() == null) throw new BadRequestException("Duur is verplicht");
        validator.validateDuration(req.getDurationMinutes());

        Customer customer = customerService.findOrCreate(req.getCustomerPhone(), req.getCustomerName());

        User staff = userRepo.findById(req.getAssignedStaffId())
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
        smsScheduler.schedule(appt, SmsType.CONFIRM);

        return AppointmentMapper.toResponse(appt);
    }

    public AppointmentResponse update(Long id, UpdateAppointmentRequest req) {
        Appointment appt = appointmentRepo.findById(id)
                .orElseThrow(() -> new NotFoundException("Afspraak niet gevonden"));

        validator.ensureModifiable(appt);

        boolean needsConflictCheck = false;
        boolean needsSms = false;

        if (req.getStartTime() != null) {
            validator.validateStartInFuture(req.getStartTime());
            appt.setStartTime(req.getStartTime());
            needsConflictCheck = true;
            needsSms = true;
        }

        if (req.getDurationMinutes() != null) {
            validator.validateDuration(req.getDurationMinutes());
            appt.setEndTime(appt.getStartTime().plusMinutes(req.getDurationMinutes()));
            needsConflictCheck = true;
            needsSms = true;
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

    public AppointmentResponse cancel(Long id, String reason) {
        Appointment appt = appointmentRepo.findById(id)
                .orElseThrow(() -> new NotFoundException("Afspraak niet gevonden"));

        if (appt.getStatus() == AppointmentStatus.CANCELED) throw new BadRequestException("Afspraak is al geannuleerd");
        if (appt.getStatus() == AppointmentStatus.COMPLETED) throw new BadRequestException("Afgeronde afspraken kunnen niet geannuleerd worden");

        appt.setStatus(AppointmentStatus.CANCELED);

        if (reason != null && !reason.isBlank()) {
            String desc = appt.getDescription() == null ? "" : appt.getDescription();
            appt.setDescription(desc + "\nAnnuleringsreden: " + reason.trim());
        }

        appt = appointmentRepo.save(appt);
        smsScheduler.schedule(appt, SmsType.CANCEL);

        return AppointmentMapper.toResponse(appt);
    }

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

    @Transactional(readOnly = true)
    public AppointmentResponse getById(Long id) {
        Appointment appt = appointmentRepo.findById(id)
                .orElseThrow(() -> new NotFoundException("Afspraak niet gevonden"));
        return AppointmentMapper.toResponse(appt);
    }

    @Transactional(readOnly = true)
    public Page<Appointment> search(String q, Pageable pageable) {
        if (q == null || q.isBlank()) return appointmentRepo.findAllByOrderByStartTimeDesc(pageable);
        return appointmentRepo.search(q.trim(), pageable);
    }
}
