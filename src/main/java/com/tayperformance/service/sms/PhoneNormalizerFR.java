package com.tayperformance.service.sms;

public final class PhoneNormalizerFR {
    private PhoneNormalizerFR() {}

    /**
     * Converteert FR nummers naar E.164 +33...
     * Voorbeelden:
     * - "06 12 34 56 78" -> "+33612345678"
     * - "0612345678"     -> "+33612345678"
     * - "+33612345678"   -> "+33612345678"
     */
    public static String toE164(String raw) {
        if (raw == null) return null;

        String s = raw.trim();
        if (s.isEmpty()) return null;

        // verwijder alles behalve + en cijfers
        s = s.replaceAll("[^\\d+]", "");

        // al E.164?
        if (s.startsWith("+")) {
            return s;
        }

        // 00 prefix -> + (internationaal)
        if (s.startsWith("00")) {
            return "+" + s.substring(2);
        }

        // FR lokaal: 0XXXXXXXXX
        if (s.startsWith("0") && s.length() == 10) {
            return "+33" + s.substring(1);
        }

        // als iemand al "33..." zonder +
        if (s.startsWith("33") && s.length() >= 11) {
            return "+" + s;
        }

        // fallback: return zoals het is (maar Twilio kan failen)
        return s;
    }
}
