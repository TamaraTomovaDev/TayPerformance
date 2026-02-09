package com.tayperformance.service.sms;

import com.tayperformance.entity.Appointment;

public interface SmsService {
    void sendConfirmation(Appointment appointment);
    void sendCancellation(Appointment appointment);
    void sendUpdate(Appointment appointment);
    void sendReminder(Appointment appointment);
}
