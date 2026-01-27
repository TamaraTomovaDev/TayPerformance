package com.tayperformance.entity;

public enum AppointmentStatus {
    REQUESTED,     // klant heeft aangevraagd (website)
    CONFIRMED,     // staff/admin heeft bevestigd
    RESCHEDULED,   // verplaatst
    CANCELED,      // geannuleerd
    COMPLETED,     // afgewerkt
    NOSHOW         // niet komen opdagen
}
