package com.tayperformance.entity;

/**
 * Type SMS bericht dat verstuurd wordt naar klanten.
 *
 * Elk type heeft een specifieke template en trigger moment.
 */
public enum SmsType {

    /**
     * BEVESTIGING van afspraak.
     *
     * Trigger: Staff/Admin bevestigt een REQUESTED afspraak.
     * Template: "Bonjour! Votre RDV est confirmé le [DATE] à [HEURE]. Adresse: [ADRES]"
     */
    CONFIRM,

    /**
     * UPDATE / wijziging van afspraak.
     *
     * Trigger: Staff/Admin wijzigt datum, tijd of service van bestaande afspraak.
     * Template: "Votre RDV a été modifié. Nouvelle date: [DATE] à [HEURE]"
     */
    UPDATE,

    /**
     * ANNULERING van afspraak.
     *
     * Trigger: Staff/Admin of klant annuleert afspraak.
     * Template: "Votre RDV du [DATE] a été annulé. Pour replanifier: [PHONE]"
     */
    CANCEL,

    /**
     * HERINNERING 24 uur voor afspraak.
     *
     * Trigger: Automatische batch job draait dagelijks.
     * Template: "Rappel: Votre RDV demain à [HEURE]. Tay Performance, [ADRES]"
     */
    REMINDER
}