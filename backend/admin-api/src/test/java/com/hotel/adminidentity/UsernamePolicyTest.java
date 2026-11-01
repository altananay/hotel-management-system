package com.hotel.adminidentity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.hotel.adminidentity.internal.domain.InvalidIdentityException;
import com.hotel.adminidentity.internal.domain.UsernamePolicy;
import org.junit.jupiter.api.Test;

class UsernamePolicyTest {

    @Test
    void shouldNormalizeToLowercaseAndTrim_whenUsernameIsValid() {
        assertThat(UsernamePolicy.normalizeAndValidate("  SuperAdmin  "))
                .isEqualTo("superadmin");
        assertThat(UsernamePolicy.normalizeAndValidate("john.doe")).isEqualTo("john.doe");
        assertThat(UsernamePolicy.normalizeAndValidate("abc")).isEqualTo("abc");
        assertThat(UsernamePolicy.normalizeAndValidate("a".repeat(40)))
                .hasSize(40);
    }

    @Test
    void shouldRejectUsername_whenLengthIsOutsideThreeToForty() {
        assertThatThrownBy(() -> UsernamePolicy.normalizeAndValidate("ab"))
                .isInstanceOf(InvalidIdentityException.class);
        assertThatThrownBy(() -> UsernamePolicy.normalizeAndValidate("a".repeat(41)))
                .isInstanceOf(InvalidIdentityException.class);
    }

    @Test
    void shouldRejectUsername_whenItDoesNotStartWithAlphanumeric() {
        assertThatThrownBy(() -> UsernamePolicy.normalizeAndValidate("-superadmin"))
                .isInstanceOf(InvalidIdentityException.class);
        assertThatThrownBy(() -> UsernamePolicy.normalizeAndValidate(".superadmin"))
                .isInstanceOf(InvalidIdentityException.class);
        assertThatThrownBy(() -> UsernamePolicy.normalizeAndValidate("_superadmin"))
                .isInstanceOf(InvalidIdentityException.class);
    }

    @Test
    void shouldRejectUsername_whenItContainsSpacesOrNonAsciiCharacters() {
        assertThatThrownBy(() -> UsernamePolicy.normalizeAndValidate("super admin"))
                .isInstanceOf(InvalidIdentityException.class);
        assertThatThrownBy(() -> UsernamePolicy.normalizeAndValidate("süperadmin"))
                .isInstanceOf(InvalidIdentityException.class);
    }

    @Test
    void shouldRejectUsername_whenItIsNull() {
        assertThatThrownBy(() -> UsernamePolicy.normalizeAndValidate(null))
                .isInstanceOf(InvalidIdentityException.class);
    }
}
