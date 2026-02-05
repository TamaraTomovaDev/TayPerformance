package com.tayperformance.entity;

/**
 * Gebruikersrollen voor het interne garage systeem.
 *
 * ⚠️ Alleen voor interne medewerkers.
 * Klanten hebben geen account en geen rol.
 */
public enum Role {

    /**
     * ADMIN - Zaakvoerder / Beheerder.
     *
     * Heeft volledige toegang tot het systeem en beheerrechten
     * over gebruikers, afspraken en configuratie.
     */
    ADMIN,

    /**
     * STAFF - Medewerker.
     *
     * Beheert dagelijkse operationele taken zoals afspraken
     * en klanten, zonder toegang tot systeeminstellingen
     * of gebruikersbeheer.
     */
    STAFF
}
