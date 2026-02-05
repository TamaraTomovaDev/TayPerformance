package com.tayperformance.entity;

/**
 * Status van een afspraak in het systeem.
 *
 * Workflow:
 * 1. REQUESTED   → Klant heeft afspraak aangevraagd (via website)
 * 2. CONFIRMED   → Staff/Admin heeft bevestigd + SMS verstuurd
 * 3. IN_PROGRESS → Service is bezig/loopt nog
 * 4. RESCHEDULED → Afspraak is verplaatst naar nieuwe datum/tijd
 * 5. COMPLETED   → Service is afgewerkt
 * 6. CANCELED    → Afspraak is geannuleerd (door klant of garage)
 * 7. NOSHOW      → Klant is niet komen opdagen
 */
public enum AppointmentStatus {

    /** Afspraak aangevraagd (via website), wacht op bevestiging */
    REQUESTED,

    /** Afspraak bevestigd door staff/admin */
    CONFIRMED,

    /** Service is bezig / in uitvoering */
    IN_PROGRESS,

    /** Afspraak is verplaatst naar nieuwe datum/tijd */
    RESCHEDULED,

    /** Afspraak is geannuleerd */
    CANCELED,

    /** Service is afgewerkt */
    COMPLETED,

    /** Klant is niet komen opdagen */
    NOSHOW
}
