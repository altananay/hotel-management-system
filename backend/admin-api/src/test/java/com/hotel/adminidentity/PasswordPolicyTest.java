package com.hotel.adminidentity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.hotel.adminidentity.internal.domain.InvalidPasswordException;
import com.hotel.adminidentity.internal.domain.PasswordPolicy;
import org.junit.jupiter.api.Test;

class PasswordPolicyTest {

    @Test
    void shouldAcceptValidPassword_whenItIsLongEnoughAndDoesNotContainIdentity() {
        String password = "Kapadokya-Guvenli-2026!";
        assertThat(PasswordPolicy.validate(password, "superadmin", "superadmin@example.com"))
                .isEqualTo(password);
    }

    @Test
    void shouldRejectShortPassword_whenFewerThanTwelveCodePoints() {
        assertThatThrownBy(() -> PasswordPolicy.validate("short", "user", "user@example.com"))
                .isInstanceOf(InvalidPasswordException.class);
    }

    @Test
    void shouldRejectPasswordContainingUsername_whenCaseInsensitive() {
        assertThatThrownBy(
                        () ->
                                PasswordPolicy.validate(
                                        "SuperAdmin-Guvenli-2026!",
                                        "superadmin",
                                        "superadmin@example.com"))
                .isInstanceOf(InvalidPasswordException.class);
    }

    @Test
    void shouldRejectPasswordContainingEmailLocalPart_whenLongEnough() {
        assertThatThrownBy(
                        () ->
                                PasswordPolicy.validate(
                                        "manager-Guvenli-2026!",
                                        "operator",
                                        "manager@example.com"))
                .isInstanceOf(InvalidPasswordException.class);
    }

    @Test
    void shouldNormalizeNfcBeforeValidation_whenPasswordUsesCombiningCharacters() {
        String decomposed = "Cafe\u0301-Guvenli-2026!";
        String validated = PasswordPolicy.validate(decomposed, "operator", "operator@example.com");
        assertThat(validated).isEqualTo("Café-Guvenli-2026!");
    }
}
