package com.tayperformance.util;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;

public final class PhoneNumberHelper {

    private static final PhoneNumberUtil PHONE_UTIL = PhoneNumberUtil.getInstance();

    private PhoneNumberHelper() {
        // utility class
    }

    /**
     * Normaliseert een telefoonnummer naar E.164 formaat.
     * @param rawNumber bv. "0470 12 34 56"
     * @param countryCode bv. "BE"
     * @return E.164 nummer of null indien ongeldig
     */
    public static String normalize(String rawNumber, String countryCode) {
        if (rawNumber == null || rawNumber.isBlank()) {
            return null;
        }

        try {
            Phonenumber.PhoneNumber number = PHONE_UTIL.parse(rawNumber, countryCode);
            if (PHONE_UTIL.isValidNumber(number)) {
                return PHONE_UTIL.format(number, PhoneNumberUtil.PhoneNumberFormat.E164);
            }
        } catch (NumberParseException ignored) {
            // ongeldig nummer
        }
        return null;
    }
}
