package com.tayperformance.service.sms;

import com.tayperformance.config.GarageProperties;
import com.tayperformance.entity.*;
import com.tayperformance.repository.SmsLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;


import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Slf4j
@Service
@RequiredArgsConstructor
public class TwilioSmsService implements SmsService {

    private final SmsLogRepository smsLogRepository;
    private final GarageProperties garage;

    @Value("${tay.sms.enabled:false}")
    private boolean smsEnabled;

    /**
     * MVP: simpele taalkeuze via property.
     * Later kan je dit per klant opslaan.
     */
    @Value("${tay.sms.language:FR}")
    private String language;
    @Value("${twilio.from:}")
    private String twilioFrom;

    @Value("${twilio.messaging-service-sid:}")
    private String messagingServiceSid;


    private static final ZoneId ZONE = ZoneId.of("Europe/Brussels");
    private static final DateTimeFormatter DATE_FR = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.FRANCE);
    private static final DateTimeFormatter TIME_FR = DateTimeFormatter.ofPattern("HH'h'mm", Locale.FRANCE);
    private static final DateTimeFormatter DATE_NL = DateTimeFormatter.ofPattern("dd/MM/yyyy", new Locale("nl", "BE"));
    private static final DateTimeFormatter TIME_NL = DateTimeFormatter.ofPattern("HH:mm", new Locale("nl", "BE"));

    @Async
    @Override
    public void sendConfirmation(Appointment appointment) {
        if (skip(appointment, SmsType.CONFIRM)) return;
        sendSms(appointment, SmsType.CONFIRM, buildConfirmationMessage(appointment));
    }

    @Async
    @Override
    public void sendCancellation(Appointment appointment) {
        if (skip(appointment, SmsType.CANCEL)) return;
        sendSms(appointment, SmsType.CANCEL, buildCancellationMessage(appointment));
    }

    @Async
    @Override
    public void sendUpdate(Appointment appointment) {
        if (skip(appointment, SmsType.UPDATE)) return;
        sendSms(appointment, SmsType.UPDATE, buildUpdateMessage(appointment));
    }

    @Async
    @Override
    public void sendReminder(Appointment appointment) {
        if (skip(appointment, SmsType.REMINDER)) return;
        sendSms(appointment, SmsType.REMINDER, buildReminderMessage(appointment));
    }

    // =========================
    // Helpers
    // =========================

    private boolean skip(Appointment appt, SmsType type) {
        if (!smsEnabled) {
            log.info("SMS disabled, skip {} appt={}", type, appt.getId());
            return true;
        }

        // ✅ Duplicate prevent (consistent for all types)
        if (smsLogRepository.hasTypeBeenSent(appt.getId(), type)) {
            log.info("SMS already sent, skip {} appt={}", type, appt.getId());
            return true;
        }

        // Safety: customer phone must exist
        if (appt.getCustomer() == null || appt.getCustomer().getPhone() == null || appt.getCustomer().getPhone().isBlank()) {
            log.warn("SMS skipped, missing customer phone appt={}", appt.getId());
            return true;
        }

        return false;
    }

    private boolean isFrench() {
        return "FR".equalsIgnoreCase(language);
    }

    private String fmtDate(OffsetDateTime dt) {
        var zdt = dt.atZoneSameInstant(ZONE);
        return isFrench() ? zdt.format(DATE_FR) : zdt.format(DATE_NL);
    }

    private String fmtTime(OffsetDateTime dt) {
        var zdt = dt.atZoneSameInstant(ZONE);
        return isFrench() ? zdt.format(TIME_FR) : zdt.format(TIME_NL);
    }

    private String fullAddress() {
        return garage.getFullAddress();
    }

    // =========================
    // Message templates
    // =========================

    private String buildConfirmationMessage(Appointment a) {
        String date = fmtDate(a.getStartTime());
        String time = fmtTime(a.getStartTime());

        if (isFrench()) {
            return String.format(
                    "TayPerformance ✅ RDV confirmé\nLe %s à %s\nAdresse: %s",
                    date, time, fullAddress()
            );
        }
        return String.format(
                "TayPerformance ✅ Afspraak bevestigd\nOp %s om %s\nAdres: %s",
                date, time, fullAddress()
        );
    }

    private String buildCancellationMessage(Appointment a) {
        String date = fmtDate(a.getStartTime());

        if (isFrench()) {
            return String.format(
                    "TayPerformance ❌ RDV annulé (prévu le %s). Besoin d'un nouveau RDV? Contact: %s",
                    date, garage.getPhone()
            );
        }
        return String.format(
                "TayPerformance ❌ Afspraak geannuleerd (op %s). Nieuwe afspraak nodig? Contact: %s",
                date, garage.getPhone()
        );
    }

    private String buildUpdateMessage(Appointment a) {
        String date = fmtDate(a.getStartTime());
        String time = fmtTime(a.getStartTime());

        if (isFrench()) {
            return String.format(
                    "TayPerformance 🔁 RDV modifié\nNouvelle date: %s à %s\nAdresse: %s",
                    date, time, fullAddress()
            );
        }
        return String.format(
                "TayPerformance 🔁 Afspraak gewijzigd\nNieuwe datum: %s om %s\nAdres: %s",
                date, time, fullAddress()
        );
    }

    private String buildReminderMessage(Appointment a) {
        String date = fmtDate(a.getStartTime());
        String time = fmtTime(a.getStartTime());

        if (isFrench()) {
            return String.format(
                    "TayPerformance ⏰ Rappel RDV\nLe %s à %s\nAdresse: %s",
                    date, time, fullAddress()
            );
        }
        return String.format(
                "TayPerformance ⏰ Herinnering afspraak\nOp %s om %s\nAdres: %s",
                date, time, fullAddress()
        );
    }

    // =========================
    // Persist + "fake send"
    // =========================

    private void sendSms(Appointment appt, SmsType type, String message) {
        String toRaw = appt.getCustomer().getPhone();
        String toPhone = PhoneNormalizerFR.toE164(toRaw);

        SmsLog logEntry = SmsLog.builder()
                .appointment(appt)
                .type(type)
                .status(SmsStatus.QUEUED)
                .toPhone(toPhone)
                .messageBody(message)
                .build();

        logEntry = smsLogRepository.save(logEntry);

        try {
            Message twilioMsg;

            if (messagingServiceSid != null && !messagingServiceSid.isBlank()) {
                twilioMsg = Message.creator(
                        new PhoneNumber(toPhone),
                        messagingServiceSid,
                        message
                ).create();
            } else {
                if (twilioFrom == null || twilioFrom.isBlank()) {
                    throw new IllegalStateException("twilio.from ontbreekt (of configureer twilio.messaging-service-sid)");
                }
                twilioMsg = Message.creator(
                        new PhoneNumber(toPhone),
                        new PhoneNumber(twilioFrom),
                        message
                ).create();
            }

            log.info("Twilio SMS sent sid={} type={} to={}", twilioMsg.getSid(), type, toPhone);

            logEntry.setStatus(SmsStatus.SENT);
            logEntry.setSentAt(OffsetDateTime.now());
            // als je veld hebt: logEntry.setProviderMessageId(twilioMsg.getSid());
            smsLogRepository.save(logEntry);

        } catch (Exception e) {
            log.error("SMS failed type={} appt={}", type, appt.getId(), e);
            logEntry.setStatus(SmsStatus.FAILED);
            logEntry.setErrorMessage(e.getMessage());
            smsLogRepository.save(logEntry);
        }
    }
}
