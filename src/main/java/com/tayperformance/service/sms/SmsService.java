package com.tayperformance.service.sms;

import com.tayperformance.entity.Appointment;

public interface SmsService {
    void sendConfirmation(Appointment appointment);
    void sendCancellation(Appointment appointment);

    default void sendUpdate(Appointment appointment) {
        // optioneel, implementations mogen dit overschrijven
    }
}
