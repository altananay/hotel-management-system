package com.hotel.adminidentity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.hotel.adminidentity.internal.domain.EmailPolicy;
import com.hotel.adminidentity.internal.domain.InvalidIdentityException;
import org.junit.jupiter.api.Test;

class EmailPolicyTest {

    @Test
    void shouldNormalizeAndAcceptCommonRfc5322Addresses() {
        assertThat(EmailPolicy.normalizeAndValidate("  SuperAdmin@Example.COM "))
                .isEqualTo("superadmin@example.com");
        assertThat(EmailPolicy.normalizeAndValidate("a@example.com"))
                .isEqualTo("a@example.com");
        assertThat(EmailPolicy.normalizeAndValidate("first.last+tag@example.co.uk"))
                .isEqualTo("first.last+tag@example.co.uk");
    }

    @Test
    void shouldRejectEmail_whenLocalPartOrAddressIsTooLong() {
        String longLocal = "a".repeat(65) + "@example.com";
        assertThatThrownBy(() -> EmailPolicy.normalizeAndValidate(longLocal))
                .isInstanceOf(InvalidIdentityException.class);

        String longAddress =
                "a".repeat(64) + "@" + "b".repeat(180) + ".com";
        assertThatThrownBy(() -> EmailPolicy.normalizeAndValidate(longAddress))
                .isInstanceOf(InvalidIdentityException.class);
    }

    @Test
    void shouldRejectEmail_whenFormatIsInvalid() {
        for (String invalid :
                new String[] {
                    "a@",
                    "@example.com",
                    "a@@example.com",
                    "a b@example.com",
                    "a..b@example.com",
                    "a@-example.com",
                    "a@example-.com",
                    "a@example.com."
                }) {
            assertThatThrownBy(() -> EmailPolicy.normalizeAndValidate(invalid))
                    .as("email should be rejected: %s", invalid)
                    .isInstanceOf(InvalidIdentityException.class);
        }
    }

    @Test
    void shouldRejectEmail_whenItIsNull() {
        assertThatThrownBy(() -> EmailPolicy.normalizeAndValidate(null))
                .isInstanceOf(InvalidIdentityException.class);
    }
}
