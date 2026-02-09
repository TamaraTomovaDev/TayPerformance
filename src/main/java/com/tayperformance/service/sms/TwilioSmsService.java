package com.tayperformance.service.sms;

import com.tayperformance.entity.*;
import com.tayperformance.repository.SmsLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Slf4j
@Service
@RequiredArgsConstructor
public class TwilioSmsService implements SmsService {

    private final SmsLogRepository smsLogRepository;

    @Value("${tay.garage.name:Tay Performance}")
    private String garageName;

    @Value("${tay.garage.address:Rue Example 123, 67000 Strasbourg}")
    private String garageAddress;

    @Value("${tay.garage.phone:+33 6 12 34 56 78}")
    private String garagePhone;

    @Value("${tay.sms.enabled:false}")
    private boolean smsEnabled;

    @Value("${tay.sms.language:FR}")
    private String language;

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.FRANCE);
    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("HH'h'mm", Locale.FRANCE);

    @Async
    @Override
    public void sendConfirmation(Appointment appointment) {
        if (!smsEnabled) { log.info("SMS disabled, skip CONFIRM {}", appointment.getId()); return; }
        if (smsLogRepository.hasTypeBeenSent(appointment.getId(), SmsType.CONFIRM)) return;

        sendSms(appointment, SmsType.CONFIRM, buildConfirmationMessage(appointment));
    }

    @Async
    @Override
    public void sendCancellation(Appointment appointment) {
        if (!smsEnabled) { log.info("SMS disabled, skip CANCEL {}", appointment.getId()); return; }
        sendSms(appointment, SmsType.CANCEL, buildCancellationMessage(appointment));
    }

    @Async
    @Override
    public void sendUpdate(Appointment appointment) {
        if (!smsEnabled) { log.info("SMS disabled, skip UPDATE {}", appointment.getId()); return; }
        sendSms(appointment, SmsType.UPDATE, buildUpdateMessage(appointment));
    }

    @Async
    @Override
    public void sendReminder(Appointment appointment) {
        if (!smsEnabled) { log.info("SMS disabled, skip REMINDER {}", appointment.getId()); return; }
        if (smsLogRepository.hasTypeBeenSent(appointment.getId(), SmsType.REMINDER)) return;

        sendSms(appointment, SmsType.REMINDER, buildReminderMessage(appointment));
    }

    private String buildConfirmationMessage(Appointment a) {
        String date = DATE_FORMAT.format(a.getStartTime());
        String time = TIME_FORMAT.format(a.getStartTime());
        if ("FR".equalsIgnoreCase(language)) {
            return String.format("Bonjour! Votre RDV %s est confirmé le %s à %s. Adresse: %s. À bientôt!",
                    garageName, date, time, garageAddress);
        }
        return String.format("Goedendag! Uw afspraak bij %s is bevestigd op %s om %s. Adres: %s. Tot binnenkort!",
                garageName, date, time, garageAddress);
    }

    private String buildCancellationMessage(Appointment a) {
        String date = DATE_FORMAT.format(a.getStartTime());
        if ("FR".equalsIgnoreCase(language)) {
            return String.format("%s: votre RDV du %s a été annulé. Pour replanifier: %s",
                    garageName, date, garagePhone);
        }
        return String.format("%s: uw afspraak van %s is geannuleerd. Om opnieuw in te plannen: %s",
                garageName, date, garagePhone);
    }

    private String buildUpdateMessage(Appointment a) {
        String date = DATE_FORMAT.format(a.getStartTime());
        String time = TIME_FORMAT.format(a.getStartTime());
        if ("FR".equalsIgnoreCase(language)) {
            return String.format("%s: votre RDV a été modifié. Nouvelle date: %s à %s. Adresse: %s",
                    garageName, date, time, garageAddress);
        }
        return String.format("%s: uw afspraak is gewijzigd. Nieuwe datum: %s om %s. Adres: %s",
                garageName, date, time, garageAddress);
    }

    private String buildReminderMessage(Appointment a) {
        String time = TIME_FORMAT.format(a.getStartTime());
        if ("FR".equalsIgnoreCase(language)) {
            return String.format("Rappel %s: RDV demain à %s. Adresse: %s. À demain!",
                    garageName, time, garageAddress);
        }
        return String.format("Herinnering %s: afspraak morgen om %s. Adres: %s. Tot morgen!",
                garageName, time, garageAddress);
    }

    private void sendSms(Appointment appt, SmsType type, String message) {
        String toPhone = appt.getCustomer().getPhone();

        SmsLog logEntry = SmsLog.builder()
                .appointment(appt)
                .type(type)
                .status(SmsStatus.QUEUED)
                .toPhone(toPhone)
                .messageBody(message)
                .build();

        logEntry = smsLogRepository.save(logEntry);

        try {
            // TODO: echte Twilio call
            log.info("SMS {} -> {}: {}", type, toPhone, message);

            logEntry.setStatus(SmsStatus.SENT);
            logEntry.setSentAt(OffsetDateTime.now());
            smsLogRepository.save(logEntry);
        } catch (Exception e) {
            log.error("SMS failed type={} appt={}", type, appt.getId(), e);
            logEntry.setStatus(SmsStatus.FAILED);
            logEntry.setErrorMessage(e.getMessage());
            smsLogRepository.save(logEntry);
        }
    }
}
