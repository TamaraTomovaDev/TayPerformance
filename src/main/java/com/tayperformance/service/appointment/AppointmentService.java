package com.tayperformance.service.appointment;

import com.tayperformance.dto.appointment.AppointmentResponse;
import com.tayperformance.entity.*;
import com.tayperformance.exception.ConflictException;
import com.tayperformance.repository.*;
import com.tayperformance.util.PhoneNumberHelper;
import com.tayperformance.service.sms.SmsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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

    private final AppointmentRepository appointmentRepository;
    private final CustomerRepository customerRepository;
    private final SmsService smsService;

    @Value("${tay.phone.default-country:BE}")
    private String defaultCountry;

    /**
     * Maakt een nieuwe afspraak aan.
     */
    @Transactional
    public AppointmentResponse createAppointment(
            Appointment appointment,
            String customerPhone,
            String customerName
    ) {
        validateBusinessRules(appointment);

        String normalizedPhone = PhoneNumberHelper.normalize(customerPhone, defaultCountry);
        if (normalizedPhone == null) {
            throw new IllegalArgumentException("Ongeldig telefoonnummer.");
        }

        checkConflicts(appointment);

        Customer customer = getUpdatedCustomer(normalizedPhone, customerName);
        appointment.setCustomer(customer);

        Appointment saved = appointmentRepository.save(appointment);
        registerSmsAfterCommit(saved, "CONFIRM");

        return AppointmentMapper.toResponse(saved);
    }

    /**
     * OPLOSSING VOOR FOUT 1: Haalt afspraken op tussen twee tijdstippen.
     */
    @Transactional(readOnly = true)
    public List<Appointment> getAppointmentsBetween(OffsetDateTime start, OffsetDateTime end) {
        return appointmentRepository.findAllByStartTimeBetweenOrderByStartTimeAsc(start, end);
    }

    /**
     * OPLOSSING VOOR FOUT 2: Annuleert een afspraak en stuurt een SMS.
     */
    @Transactional
    public void cancelAppointment(Long id) {
        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Afspraak niet gevonden met id: " + id));

        appointment.setStatus(AppointmentStatus.CANCELED);
        appointmentRepository.save(appointment);

        registerSmsAfterCommit(appointment, "CANCEL");
    }

    // --- Private Helper Methoden ---

    private void validateBusinessRules(Appointment appointment) {
        if (appointment.getStartTime().isBefore(OffsetDateTime.now())) {
            throw new IllegalArgumentException("Afspraak kan niet in het verleden liggen.");
        }
        if (!appointment.getEndTime().isAfter(appointment.getStartTime())) {
            throw new IllegalArgumentException("Eindtijd moet na starttijd liggen.");
        }
    }

    private void checkConflicts(Appointment appointment) {
        List<Appointment> conflicts = appointmentRepository.findConflictingAppointments(
                appointment.getStartTime(), appointment.getEndTime()
        );
        if (!conflicts.isEmpty()) {
            Appointment c = conflicts.get(0);
            throw new ConflictException("Tijdslot bezet", c.getId(), c.getStartTime(), c.getCarBrand());
        }
    }

    private Customer getUpdatedCustomer(String phone, String name) {
        return customerRepository.findByPhoneAndActiveTrue(phone)
                .map(c -> {
                    if (name != null && !name.isBlank() && !name.equals(c.getFirstName())) {
                        c.setFirstName(name);
                    }
                    return c;
                })
                .orElseGet(() -> customerRepository.save(
                        Customer.builder().phone(phone).firstName(name).active(true).build()
                ));
    }

    private void registerSmsAfterCommit(Appointment app, String type) {
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    if ("CONFIRM".equals(type)) smsService.sendConfirmation(app);
                    else smsService.sendCancellation(app);
                }
            });
        }
    }
}
