package com.tayperformance.controller.publicapi;

import com.tayperformance.dto.appointment.AppointmentRequest;
import com.tayperformance.dto.appointment.AppointmentResponse;
import com.tayperformance.entity.Appointment;
import com.tayperformance.service.appointment.AppointmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controller voor de publieke website van Tay Performance.
 * Hier kunnen klanten zonder inloggen een afspraak boeken.
 */
@RestController
@RequestMapping("/api/public/bookings")
@RequiredArgsConstructor
@CrossOrigin(origins = "*") // Zorgt dat de website mag communiceren met de API
public class BookingController {

    private final AppointmentService appointmentService;

    @PostMapping("/request")
    public ResponseEntity<AppointmentResponse> publicBooking(
            @Valid @RequestBody AppointmentRequest request
    ) {
        Appointment appointment = Appointment.builder()
                .carBrand(request.getCarBrand())
                .carModel(request.getCarModel())
                .description(request.getDescription())
                .price(request.getPrice())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .build();

        AppointmentResponse saved = appointmentService.createAppointment(
                appointment,
                request.getCustomerPhone(),
                request.getCustomerName()
        );

        return new ResponseEntity<>(saved, HttpStatus.CREATED);
    }

}
