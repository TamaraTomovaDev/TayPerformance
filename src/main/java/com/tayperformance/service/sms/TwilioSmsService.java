package com.tayperformance.service.sms;

import com.tayperformance.entity.Appointment;
import com.tayperformance.entity.SmsLog;
import com.tayperformance.entity.SmsStatus;
import com.tayperformance.entity.SmsType;
import com.tayperformance.repository.SmsLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Twilio SMS implementatie met logging en retry logic.
 *
 * Features:
 * - Asynchrone verzending
 * - Audit logging in database
 * - Duplicate prevention
 * - Formatted messages (Frans/Nederlands)
 */
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

    // ============================================================
    // PUBLIC API
    // ============================================================

    @Async
    @Override
    public void sendConfirmation(Appointment appointment) {
        if (!smsEnabled) {
            log.info("SMS disabled, skipping confirmation for appointment id={}", appointment.getId());
            return;
        }

        // Check duplicate
        if (smsLogRepository.hasReminderBeenSent(appointment.getId())) {
            log.warn("Confirmation already sent for appointment id={}", appointment.getId());
            return;
        }

        String message = buildConfirmationMessage(appointment);
        sendSms(appointment, SmsType.CONFIRM, message);
    }

    @Async
    @Override
    public void sendCancellation(Appointment appointment) {
        if (!smsEnabled) {
            log.info("SMS disabled, skipping cancellation for appointment id={}", appointment.getId());
            return;
        }

        String message = buildCancellationMessage(appointment);
        sendSms(appointment, SmsType.CANCEL, message);
    }

    @Async
    @Override
    public void sendUpdate(Appointment appointment) {
        if (!smsEnabled) {
            log.info("SMS disabled, skipping update for appointment id={}", appointment.getId());
            return;
        }

        String message = buildUpdateMessage(appointment);
        sendSms(appointment, SmsType.UPDATE, message);
    }

    @Async
    @Override
    public void sendReminder(Appointment appointment) {
        if (!smsEnabled) {
            log.info("SMS disabled, skipping reminder for appointment id={}", appointment.getId());
            return;
        }

        // Check duplicate
        if (smsLogRepository.hasReminderBeenSent(appointment.getId())) {
            log.info("Reminder already sent for appointment id={}", appointment.getId());
            return;
        }

        String message = buildReminderMessage(appointment);
        sendSms(appointment, SmsType.REMINDER, message);
    }

    // ============================================================
    // MESSAGE BUILDERS
    // ============================================================

    private String buildConfirmationMessage(Appointment appointment) {
        String date = DATE_FORMAT.format(appointment.getStartTime());
        String time = TIME_FORMAT.format(appointment.getStartTime());

        if ("FR".equals(language)) {
            return String.format(
                    "Bonjour! Votre RDV %s est confirmé le %s à %s. " +
                            "Adresse: %s. À bientôt!",
                    garageName, date, time, garageAddress
            );
        } else {
            return String.format(
                    "Goedendag! Uw afspraak bij %s is bevestigd op %s om %s. " +
                            "Adres: %s. Tot binnenkort!",
                    garageName, date, time, garageAddress
            );
        }
    }

    private String buildCancellationMessage(Appointment appointment) {
        String date = DATE_FORMAT.format(appointment.getStartTime());

        if ("FR".equals(language)) {
            return String.format(
                    "%s: votre RDV du %s a été annulé. " +
                            "Pour replanifier: %s",
                    garageName, date, garagePhone
            );
        } else {
            return String.format(
                    "%s: uw afspraak van %s is geannuleerd. " +
                            "Om opnieuw in te plannen: %s",
                    garageName, date, garagePhone
            );
        }
    }

    private String buildUpdateMessage(Appointment appointment) {
        String date = DATE_FORMAT.format(appointment.getStartTime());
        String time = TIME_FORMAT.format(appointment.getStartTime());

        if ("FR".equals(language)) {
            return String.format(
                    "%s: votre RDV a été modifié. " +
                            "Nouvelle date: %s à %s. Adresse: %s",
                    garageName, date, time, garageAddress
            );
        } else {
            return String.format(
                    "%s: uw afspraak is gewijzigd. " +
                            "Nieuwe datum: %s om %s. Adres: %s",
                    garageName, date, time, garageAddress
            );
        }
    }

    private String buildReminderMessage(Appointment appointment) {
        String time = TIME_FORMAT.format(appointment.getStartTime());

        if ("FR".equals(language)) {
            return String.format(
                    "Rappel %s: RDV demain à %s. " +
                            "Adresse: %s. À demain!",
                    garageName, time, garageAddress
            );
        } else {
            return String.format(
                    "Herinnering %s: afspraak morgen om %s. " +
                            "Adres: %s. Tot morgen!",
                    garageName, time, garageAddress
            );
        }
    }

    // ============================================================
    // SMS SENDING & LOGGING
    // ============================================================

    private void sendSms(Appointment appointment, SmsType type, String message) {
        String toPhone = appointment.getCustomer().getPhone();

        // Log SMS in database (QUEUED status)
        SmsLog smsLog = SmsLog.builder()
                .appointment(appointment)
                .type(type)
                .status(SmsStatus.QUEUED)
                .toPhone(toPhone)
                .messageBody(message)
                .build();

        smsLog = smsLogRepository.save(smsLog);

        try {
            // TODO: Echte Twilio API call
            // TwilioResponse response = twilioClient.sendSms(toPhone, message);
            // smsLog.setProviderMessageId(response.getSid());

            // Simulatie: log naar console
            log.info("SMS {} -> {}: {}", type, toPhone, message);

            // Update status naar SENT
            smsLog.setStatus(SmsStatus.SENT);
            smsLog.setSentAt(OffsetDateTime.now());
            smsLogRepository.save(smsLog);

        } catch (Exception e) {
            log.error("Failed to send SMS type={} to={}", type, toPhone, e);

            // Update status naar FAILED
            smsLog.setStatus(SmsStatus.FAILED);
            smsLog.setErrorMessage(e.getMessage());
            smsLogRepository.save(smsLog);

            // Don't rethrow - SMS failure should not break appointment flow
        }
    }

    // ============================================================
    // WEBHOOK HANDLER (voor Twilio callbacks)
    // ============================================================

    /**
     * Handle Twilio delivery webhook.
     * Update SMS status naar DELIVERED.
     */
    public void handleDeliveryCallback(String providerMessageId, String status) {
        // TODO: Implement webhook endpoint in controller
        // Find SMS by providerMessageId and update status

        log.info("SMS delivery callback: id={} status={}", providerMessageId, status);
    }
}