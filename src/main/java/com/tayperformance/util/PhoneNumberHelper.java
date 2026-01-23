package com.tayperformance.util;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;

public class PhoneNumberHelper {

    private static final PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();

    public static String normalize(String rawNumber, String countryCode) {
        try {
            Phonenumber.PhoneNumber number = phoneUtil.parse(rawNumber, countryCode);
            if (phoneUtil.isValidNumber(number)) {
                return phoneUtil.format(number, PhoneNumberUtil.PhoneNumberFormat.E164);
            }
        } catch (NumberParseException e) {
            // Ongeldig nummer
        }
        return null;
    }
}