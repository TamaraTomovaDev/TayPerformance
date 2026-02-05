package com.tayperformance.service.sms;

import com.tayperformance.entity.Appointment;

/**
 * Interface voor SMS communicatie met klanten.
 *
 * Implementaties:
 * - TwilioSmsService (productie)
 * - MockSmsService (testing)
 */
public interface SmsService {

    /**
     * Verstuur bevestiging van afspraak.
     * Bevat: datum, tijd, adres.
     */
    void sendConfirmation(Appointment appointment);

    /**
     * Verstuur annulatie bericht.
     */
    void sendCancellation(Appointment appointment);

    /**
     * Verstuur update bericht (reschedule).
     */
    void sendUpdate(Appointment appointment);

    /**
     * Verstuur reminder 24u voor afspraak.
     */
    void sendReminder(Appointment appointment);
}