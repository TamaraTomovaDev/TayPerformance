package com.tayperformance.service.appointment;

import com.tayperformance.dto.appointment.AppointmentResponse;
import com.tayperformance.dto.appointment.CreateAppointmentRequest;
import com.tayperformance.dto.appointment.UpdateAppointmentRequest;
import com.tayperformance.entity.*;
import com.tayperformance.exception.BadRequestException;
import com.tayperformance.exception.NotFoundException;
import com.tayperformance.mapper.AppointmentMapper;
import com.tayperformance.repository.AppointmentRepository;
import com.tayperformance.repository.CustomerRepository;
import com.tayperformance.repository.DetailServiceRepository;
import com.tayperformance.repository.UserRepository;
import com.tayperformance.service.appointment.core.AppointmentConflictChecker;
import com.tayperformance.service.appointment.core.AppointmentValidator;
import com.tayperformance.service.appointment.core.AppointmentSmsScheduler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class AppointmentService {

    private final AppointmentRepository appointmentRepo;
    private final CustomerRepository customerRepo;
    private final DetailServiceRepository serviceRepo;
    private final UserRepository userRepo;

    private final AppointmentValidator validator;
    private final AppointmentConflictChecker conflictChecker;
    private final AppointmentSmsScheduler smsScheduler;

    // ============================================================
    // CREATE - PUBLIEKE WEBSITE (REQUESTED)
    // ============================================================

    /**
     * Publieke website - klant vraagt afspraak aan.
     * Status = REQUESTED, moet nog bevestigd worden door staff.
     */
    public AppointmentResponse createPublicRequest(CreateAppointmentRequest req) {
        log.info("Creating public appointment request for phone: {}", req.getCustomerPhone());

        // Validatie
        validator.validateStartInFuture(req.getStartTime());
        if (req.getServiceId() == null) {
            throw new BadRequestException("Service is verplicht voor publieke booking");
        }

        // Ophalen/aanmaken customer
        Customer customer = customerRepo.findByPhoneAndActiveTrue(req.getCustomerPhone())
                .orElseGet(() -> createNewCustomer(req));

        // Ophalen service
        DetailService service = serviceRepo.findById(req.getServiceId())
                .orElseThrow(() -> new NotFoundException("Service niet gevonden"));

        // Berekenen endTime obv service duur
        OffsetDateTime endTime = req.getStartTime().plusMinutes(service.getDefaultMinutes());

        // Maak appointment
        Appointment appointment = Appointment.builder()
                .customer(customer)
                .service(service)
                .carBrand(req.getCarBrand())
                .carModel(req.getCarModel())
                .description(req.getDescription())
                .startTime(req.getStartTime())
                .endTime(endTime)
                .status(AppointmentStatus.REQUESTED)
                .build();

        appointment = appointmentRepo.save(appointment);

        log.info("Created appointment request with ID: {}", appointment.getId());
        return AppointmentMapper.toResponse(appointment);
    }

    // ============================================================
    // CREATE - INTERNE APP (DIRECT CONFIRMED)
    // ============================================================

    /**
     * Interne app - staff maakt direct bevestigde afspraak.
     * Status = CONFIRMED, klaar om uitgevoerd te worden.
     */
    public AppointmentResponse createConfirmedAppointment(CreateAppointmentRequest req) {
        log.info("Creating confirmed appointment for phone: {}", req.getCustomerPhone());

        // Validatie - interne app vereist meer velden
        validator.validateStartInFuture(req.getStartTime());
        if (req.getPrice() == null) {
            throw new BadRequestException("Prijs is verplicht voor directe booking");
        }
        if (req.getAssignedStaffId() == null) {
            throw new BadRequestException("Staff is verplicht voor directe booking");
        }
        if (req.getDurationMinutes() == null) {
            throw new BadRequestException("Duur is verplicht voor directe booking");
        }
        validator.validateDuration(req.getDurationMinutes());

        // Ophalen entities
        Customer customer = customerRepo.findByPhoneAndActiveTrue(req.getCustomerPhone())
                .orElseGet(() -> createNewCustomer(req));

        User staff = userRepo.findById(req.getAssignedStaffId())
                .orElseThrow(() -> new NotFoundException("Staff niet gevonden"));

        DetailService service = req.getServiceId() != null
                ? serviceRepo.findById(req.getServiceId())
                .orElseThrow(() -> new NotFoundException("Service niet gevonden"))
                : null;

        // Berekenen endTime
        OffsetDateTime endTime = req.getStartTime().plusMinutes(req.getDurationMinutes());

        // Maak appointment
        Appointment appointment = Appointment.builder()
                .customer(customer)
                .service(service)
                .assignedStaff(staff)
                .carBrand(req.getCarBrand())
                .carModel(req.getCarModel())
                .description(req.getDescription())
                .startTime(req.getStartTime())
                .endTime(endTime)
                .price(req.getPrice())
                .status(AppointmentStatus.CONFIRMED)
                .build();

        // Check conflicts
        conflictChecker.ensureNoConflict(appointment);

        appointment = appointmentRepo.save(appointment);

        // Schedule SMS
        smsScheduler.schedule(appointment, SmsType.CONFIRM);

        log.info("Created confirmed appointment with ID: {}", appointment.getId());
        return AppointmentMapper.toResponse(appointment);
    }

    // ============================================================
    // UPDATE - GENERIC (alle velden optioneel)
    // ============================================================

    /**
     * Update appointment details.
     * Alleen ingevulde velden worden geüpdatet.
     */
    public AppointmentResponse update(Long id, UpdateAppointmentRequest req) {
        log.info("Updating appointment {}", id);

        Appointment appointment = appointmentRepo.findById(id)
                .orElseThrow(() -> new NotFoundException("Afspraak niet gevonden"));

        validator.ensureModifiable(appointment);

        boolean needsConflictCheck = false;
        boolean needsSms = false;

        // Update velden als ze ingevuld zijn
        if (req.getStartTime() != null) {
            validator.validateStartInFuture(req.getStartTime());
            appointment.setStartTime(req.getStartTime());
            needsConflictCheck = true;
            needsSms = true;
        }

        if (req.getDurationMinutes() != null) {
            validator.validateDuration(req.getDurationMinutes());
            appointment.setEndTime(appointment.getStartTime().plusMinutes(req.getDurationMinutes()));
            needsConflictCheck = true;
            needsSms = true;
        }

        if (req.getAssignedStaffId() != null) {
            User staff = userRepo.findById(req.getAssignedStaffId())
                    .orElseThrow(() -> new NotFoundException("Staff niet gevonden"));
            appointment.setAssignedStaff(staff);
            needsConflictCheck = true;
        }

        if (req.getPrice() != null) {
            appointment.setPrice(req.getPrice());
        }

        if (req.getDescription() != null) {
            appointment.setDescription(req.getDescription());
        }

        if (req.getCarBrand() != null) {
            appointment.setCarBrand(req.getCarBrand());
        }

        if (req.getCarModel() != null) {
            appointment.setCarModel(req.getCarModel());
        }

        // Conflict check als tijd/staff gewijzigd is
        if (needsConflictCheck) {
            conflictChecker.ensureNoConflict(appointment);
        }

        appointment = appointmentRepo.save(appointment);

        // SMS als tijd gewijzigd is
        if (needsSms) {
            smsScheduler.schedule(appointment, SmsType.UPDATE);
        }

        log.info("Updated appointment {}", id);
        return AppointmentMapper.toResponse(appointment);
    }

    // ============================================================
    // CONFIRM - REQUESTED → CONFIRMED
    // ============================================================

    /**
     * Bevestig een REQUESTED appointment.
     * Wijst staff toe, zet prijs en duur, changes status naar CONFIRMED.
     */
    public AppointmentResponse confirmRequest(Long id, UpdateAppointmentRequest req) {
        log.info("Confirming appointment request {}", id);

        Appointment appointment = appointmentRepo.findById(id)
                .orElseThrow(() -> new NotFoundException("Afspraak niet gevonden"));

        if (appointment.getStatus() != AppointmentStatus.REQUESTED) {
            throw new BadRequestException("Alleen REQUESTED afspraken kunnen bevestigd worden");
        }

        // Validatie - deze velden zijn verplicht voor confirmation
        if (req.getAssignedStaffId() == null) {
            throw new BadRequestException("Staff is verplicht");
        }
        if (req.getPrice() == null) {
            throw new BadRequestException("Prijs is verplicht");
        }
        if (req.getDurationMinutes() == null) {
            throw new BadRequestException("Duur is verplicht");
        }
        validator.validateDuration(req.getDurationMinutes());

        // Assign staff
        User staff = userRepo.findById(req.getAssignedStaffId())
                .orElseThrow(() -> new NotFoundException("Staff niet gevonden"));
        appointment.setAssignedStaff(staff);

        // Set price & duration
        appointment.setPrice(req.getPrice());
        appointment.setEndTime(appointment.getStartTime().plusMinutes(req.getDurationMinutes()));

        // Check conflicts
        conflictChecker.ensureNoConflict(appointment);

        // Change status
        appointment.setStatus(AppointmentStatus.CONFIRMED);

        appointment = appointmentRepo.save(appointment);

        // SMS
        smsScheduler.schedule(appointment, SmsType.CONFIRM);

        log.info("Confirmed appointment {}", id);
        return AppointmentMapper.toResponse(appointment);
    }

    // ============================================================
    // CANCEL
    // ============================================================

    public AppointmentResponse cancel(Long id, String reason) {
        log.info("Canceling appointment {}", id);

        Appointment appointment = appointmentRepo.findById(id)
                .orElseThrow(() -> new NotFoundException("Afspraak niet gevonden"));

        if (appointment.getStatus() == AppointmentStatus.CANCELED) {
            throw new BadRequestException("Afspraak is al geannuleerd");
        }
        if (appointment.getStatus() == AppointmentStatus.COMPLETED) {
            throw new BadRequestException("Afgeronde afspraken kunnen niet geannuleerd worden");
        }

        appointment.setStatus(AppointmentStatus.CANCELED);
        if (reason != null) {
            appointment.setDescription(appointment.getDescription() + "\nAnnuleringsreden: " + reason);
        }

        appointment = appointmentRepo.save(appointment);

        // SMS
        smsScheduler.schedule(appointment, SmsType.CANCEL);

        log.info("Canceled appointment {}", id);
        return AppointmentMapper.toResponse(appointment);
    }

    // ============================================================
    // STATUS TRANSITIONS
    // ============================================================

    public AppointmentResponse markInProgress(Long id) {
        return transitionStatus(id, AppointmentStatus.IN_PROGRESS);
    }

    public AppointmentResponse markCompleted(Long id) {
        return transitionStatus(id, AppointmentStatus.COMPLETED);
    }

    public AppointmentResponse markNoShow(Long id) {
        return transitionStatus(id, AppointmentStatus.NOSHOW);
    }

    private AppointmentResponse transitionStatus(Long id, AppointmentStatus newStatus) {
        Appointment appointment = appointmentRepo.findById(id)
                .orElseThrow(() -> new NotFoundException("Afspraak niet gevonden"));

        appointment.setStatus(newStatus);
        appointment = appointmentRepo.save(appointment);

        log.info("Transitioned appointment {} to status {}", id, newStatus);
        return AppointmentMapper.toResponse(appointment);
    }

    // ============================================================
    // READ OPERATIONS
    // ============================================================

    public AppointmentResponse getById(Long id) {
        Appointment appointment = appointmentRepo.findById(id)
                .orElseThrow(() -> new NotFoundException("Afspraak niet gevonden"));
        return AppointmentMapper.toResponse(appointment);
    }

    public Page<Appointment> search(String searchTerm, Pageable pageable) {
        if (searchTerm == null || searchTerm.isBlank()) {
            return appointmentRepo.findAllByOrderByStartTimeDesc(pageable);
        }
        return appointmentRepo.searchByCustomer(searchTerm, pageable);
    }

    // ============================================================
    // HELPER METHODS
    // ============================================================

    private Customer createNewCustomer(CreateAppointmentRequest req) {
        log.info("Creating new customer for phone: {}", req.getCustomerPhone());

        String[] nameParts = req.getCustomerName() != null
                ? req.getCustomerName().split(" ", 2)
                : new String[]{"", ""};

        Customer customer = Customer.builder()
                .phone(req.getCustomerPhone())
                .firstName(nameParts[0])
                .lastName(nameParts.length > 1 ? nameParts[1] : "")
                .active(true)
                .build();

        return customerRepo.save(customer);
    }
}