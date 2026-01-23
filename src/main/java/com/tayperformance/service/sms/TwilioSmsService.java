package com.tayperformance.service.sms;

import com.tayperformance.entity.Appointment;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class TwilioSmsService implements SmsService {

    // В 2026 году лучше выносить тексты сообщений в конфиг или базу
    private static final String ADDRESS = "Straatnaam 123, 1000 Brussel";

    @Async // Это позволяет методу выполняться в фоновом потоке
    @Override
    public void sendConfirmation(Appointment appointment) {
        String phone = appointment.getCustomer().getPhone();
        String message = String.format(
                "Bevestiging Tay Performance: Je afspraak staat gepland op %s voor je %s. Adres: %s.",
                appointment.getStartTime().toString(), // Здесь можно отформатировать дату покрасивее
                appointment.getCarBrand(),
                ADDRESS
        );

        try {
            // Имитация вызова внешнего API (Twilio, MessageBird и т.д.)
            log.info("Verzenden SMS naar {}: {}", phone, message);

            // Здесь будет реальный код:
            // twilioClient.messages.create(new PhoneNumber(phone), ...)

        } catch (Exception e) {
            log.error("Fout bij verzenden SMS naar {}", phone, e);
        }
    }

    @Async
    @Override
    public void sendCancellation(Appointment appointment) {
        String phone = appointment.getCustomer().getPhone();
        String message = "Tay Performance: Je afspraak op " + appointment.getStartTime() + " is geannuleerd.";

        log.info("Verzenden annulatie SMS naar {}: {}", phone, message);
    }
}
