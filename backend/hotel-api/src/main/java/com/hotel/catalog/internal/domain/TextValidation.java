package com.hotel.catalog.internal.domain;

import com.hotel.sharedkernel.api.DomainError;
import com.hotel.sharedkernel.api.DomainErrorCategory;

final class TextValidation {

    private TextValidation() {}

    static String validateAndTrim(
            String raw,
            int maxLength,
            String requiredCode,
            String invalidCode,
            String fieldLabel) {
        if (raw == null) {
            throw new DomainError(
                    requiredCode,
                    fieldLabel + " is required and must not be blank.",
                    DomainErrorCategory.VALIDATION);
        }
        if (containsForbiddenCharacters(raw)) {
            throw new DomainError(
                    invalidCode,
                    fieldLabel
                            + " must not contain HTML angle brackets or control characters.",
                    DomainErrorCategory.VALIDATION);
        }
        String trimmed = raw.strip();
        if (trimmed.isEmpty()) {
            throw new DomainError(
                    requiredCode,
                    fieldLabel + " is required and must not be blank.",
                    DomainErrorCategory.VALIDATION);
        }
        if (trimmed.length() > maxLength) {
            throw new DomainError(
                    invalidCode,
                    fieldLabel + " must be between 1 and " + maxLength + " characters.",
                    DomainErrorCategory.VALIDATION);
        }
        return trimmed;
    }

    private static boolean containsForbiddenCharacters(String value) {
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            if (ch == '<' || ch == '>' || ch <= '\u001F' || (ch >= '\u007F' && ch <= '\u009F')) {
                return true;
            }
        }
        return false;
    }
}
