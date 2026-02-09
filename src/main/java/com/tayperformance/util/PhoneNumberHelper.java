package com.tayperformance.util;

public class PhoneNumberHelper {

    // MVP: basic normalization
    public static String normalize(String raw, String defaultCountry) {
        if (raw == null) return null;

        String p = raw.trim().replace(" ", "").replace("-", "");
        if (p.isBlank()) return null;

        // If user already gives +33..., keep it
        if (p.startsWith("+")) return p;

        // Very simple EU default (BE example):
        // "04..." -> "+32..."
        if ("BE".equalsIgnoreCase(defaultCountry)) {
            if (p.startsWith("0")) p = p.substring(1);
            return "+32" + p;
        }

        // fallback: just return as-is (but better later with libphonenumber)
        return p;
    }
}
