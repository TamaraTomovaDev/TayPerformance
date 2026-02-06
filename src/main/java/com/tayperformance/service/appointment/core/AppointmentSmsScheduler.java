package com.tayperformance.service.appointment.core;

import com.tayperformance.entity.Appointment;
import com.tayperformance.entity.SmsType;
import com.tayperformance.service.sms.SmsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
@RequiredArgsConstructor
@Slf4j
public class AppointmentSmsScheduler {

    private final SmsService smsService;

    /**
     * Stuurt SMS *na* succesvolle transacties via afterCommit()
     */
    public void schedule(Appointment appt, SmsType type) {

        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            log.warn("No active transaction. SMS for appointment {} skipped.", appt.getId());
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        try {
                            switch (type) {
                                case CONFIRM -> smsService.sendConfirmation(appt);
                                case CANCEL -> smsService.sendCancellation(appt);
                                case UPDATE -> smsService.sendUpdate(appt);
                                default -> log.warn("Unknown SMS type {}", type);
                            }
                        } catch (Exception ex) {
                            log.error("Failed to send SMS for appointment {} type {}", appt.getId(), type, ex);
                        }
                    }
                }
        );
    }
}