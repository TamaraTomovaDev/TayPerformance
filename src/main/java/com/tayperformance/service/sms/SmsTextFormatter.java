package com.tayperformance.service.sms;

import com.tayperformance.config.GarageProperties;
import com.tayperformance.entity.Appointment;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public final class SmsTextFormatter {

    private static final ZoneId ZONE = ZoneId.of("Europe/Brussels");
    private static final Locale LOCALE_FR = Locale.FRANCE;

    private static final DateTimeFormatter DATE_FR =
            DateTimeFormatter.ofPattern("dd/MM/yyyy", LOCALE_FR);

    private static final DateTimeFormatter TIME_FR =
            DateTimeFormatter.ofPattern("HH:mm", LOCALE_FR);

    private SmsTextFormatter() {}

    public static String confirmation(Appointment appt, GarageProperties garage) {
        String date = appt.getStartTime().atZoneSameInstant(ZONE).format(DATE_FR);
        String time = appt.getStartTime().atZoneSameInstant(ZONE).format(TIME_FR);

        return """
TayPerformance ✅ RDV confirmé
Le %s à %s
Adresse: %s
""".formatted(date, time, garage.getFullAddress()).trim();
    }

    public static String update(Appointment appt, GarageProperties garage) {
        String date = appt.getStartTime().atZoneSameInstant(ZONE).format(DATE_FR);
        String time = appt.getStartTime().atZoneSameInstant(ZONE).format(TIME_FR);

        return """
TayPerformance 🔁 RDV modifié
Le %s à %s
Adresse: %s
""".formatted(date, time, garage.getFullAddress()).trim();
    }

    public static String cancellation(Appointment appt, GarageProperties garage) {
        return "TayPerformance ❌ RDV annulé. Si besoin, reprenez un rendez-vous via notre site.";
    }

    public static String reminder(Appointment appt, GarageProperties garage) {
        String date = appt.getStartTime().atZoneSameInstant(ZONE).format(DATE_FR);
        String time = appt.getStartTime().atZoneSameInstant(ZONE).format(TIME_FR);

        return """
TayPerformance ⏰ Rappel RDV
Le %s à %s
Adresse: %s
""".formatted(date, time, garage.getFullAddress()).trim();
    }
}
