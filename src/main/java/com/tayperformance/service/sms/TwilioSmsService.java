package com.tayperformance.service.sms;

import com.tayperformance.entity.Appointment;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class TwilioSmsService implements SmsService {

    @Value("${tay.garage.address:Straatnaam 123, 1000 Brussel}")
    private String address;

    @Async
    @Override
    public void sendConfirmation(Appointment appointment) {
        String phone = appointment.getCustomer().getPhone();

        String message = String.format(
                "Tay Performance bevestiging: afspraak op %s voor %s. Adres: %s.",
                appointment.getStartTime(),
                appointment.getCarBrand(),
                address
        );

        try {
            log.info("SMS bevestiging -> {}: {}", phone, message);
            // TODO: echte provider call (Twilio/MessageBird)
        } catch (Exception e) {
            log.error("Fout bij verzenden SMS bevestiging naar {}", phone, e);
        }
    }

    @Async
    @Override
    public void sendCancellation(Appointment appointment) {
        String phone = appointment.getCustomer().getPhone();
        String message = "Tay Performance: je afspraak op " + appointment.getStartTime() + " is geannuleerd.";

        try {
            log.info("SMS annulatie -> {}: {}", phone, message);
        } catch (Exception e) {
            log.error("Fout bij verzenden SMS annulatie naar {}", phone, e);
        }
    }
}
