package com.tayperformance.service.appointment;

import com.tayperformance.entity.*;
import com.tayperformance.exception.ConflictException;
import com.tayperformance.repository.AppointmentRepository;
import com.tayperformance.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional
public class AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final CustomerRepository customerRepository;

    public Appointment createAppointment(Appointment appointment, String customerPhone, String customerName) {

        // Basis validatie tijd
        if (appointment.getStartTime() == null || appointment.getEndTime() == null) {
            throw new IllegalArgumentException("Start- en eindtijd zijn verplicht");
        }

        if (!appointment.getEndTime().isAfter(appointment.getStartTime())) {
            throw new IllegalArgumentException("Eindtijd moet na starttijd liggen");
        }

        // Conflict-check (vrije uren, geen berekening)
        boolean conflict = appointmentRepository
                .existsByStartTimeLessThanAndEndTimeGreaterThanAndStatusNot(
                        appointment.getEndTime(),
                        appointment.getStartTime(),
                        AppointmentStatus.CANCELED
                );

        if (conflict) {
            throw new ConflictException("Dit tijdslot is al bezet");
        }

        // Klant ophalen of aanmaken (op telefoonnummer)
        Customer customer = customerRepository
                .findByPhone(customerPhone)
                .orElseGet(() -> {
                    Customer newCustomer = new Customer();
                    newCustomer.setPhone(customerPhone);
                    newCustomer.setFirstName(customerName);
                    return customerRepository.save(newCustomer);
                });

        // Afspraak opbouwen
        appointment.setCustomer(customer);
        appointment.setStatus(AppointmentStatus.CONFIRMED);

        // Opslaan
        return appointmentRepository.save(appointment);
    }
}
