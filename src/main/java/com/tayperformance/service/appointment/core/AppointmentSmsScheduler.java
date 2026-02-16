package com.tayperformance.service.appointment.core;

import com.tayperformance.entity.Appointment;
import com.tayperformance.entity.SmsType;
import com.tayperformance.service.sms.SmsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Component
@RequiredArgsConstructor
public class AppointmentSmsScheduler {

    private final SmsService smsService;

    public void schedule(Appointment appt, SmsType type) {

        // ✅ verstuur pas nadat de transactie committed is
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    sendNow(appt, type);
                }
            });
        } else {
            // fallback (zou zelden gebeuren)
            sendNow(appt, type);
        }
    }

    private void sendNow(Appointment appt, SmsType type) {
        switch (type) {
            case CONFIRM -> smsService.sendConfirmation(appt);
            case UPDATE -> smsService.sendUpdate(appt);
            case CANCEL -> smsService.sendCancellation(appt);
            case REMINDER -> smsService.sendReminder(appt);
        }
    }
}
