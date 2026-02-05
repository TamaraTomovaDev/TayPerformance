package com.tayperformance.entity;

/**
 * Status van een SMS bericht in het systeem.
 *
 * Lifecycle van een SMS:
 * 1. QUEUED    → Net aangemaakt, wacht op verzending
 * 2. SENT      → Verzonden naar SMS provider (Twilio/Vonage)
 * 3. DELIVERED → Bevestiging ontvangen dat SMS bij klant is aangekomen
 * 4. FAILED    → Fout opgetreden, SMS niet verstuurd
 */
public enum SmsStatus {

    /**
     * SMS staat in de wachtrij om verstuurd te worden.
     * Dit is de initiële status bij het aanmaken.
     */
    QUEUED,

    /**
     * SMS is verstuurd naar de SMS provider.
     * Provider heeft de SMS geaccepteerd voor verzending.
     * Nog geen bevestiging van levering.
     */
    SENT,

    /**
     * SMS is succesvol afgeleverd bij de klant.
     * Bevestigd via webhook van de SMS provider.
     * Dit is de finale succes status.
     */
    DELIVERED,

    /**
     * SMS kon niet verstuurd worden.
     * Mogelijke redenen:
     * - Ongeldig telefoonnummer
     * - Nummer geblokkeerd
     * - Onvoldoende tegoed bij provider
     * - Provider technische fout
     *
     * Zie SmsLog.errorMessage voor details.
     */
    FAILED
}