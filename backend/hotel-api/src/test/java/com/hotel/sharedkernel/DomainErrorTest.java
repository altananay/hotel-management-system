package com.hotel.sharedkernel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.hotel.sharedkernel.api.DomainError;
import com.hotel.sharedkernel.api.DomainErrorCategory;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link DomainError} (AC-4, CR-6).
 */
class DomainErrorTest {

    @Test
    void shouldStoreModulePrefixedSnakeCaseCodeAndEnglishMessage_whenConstructed() {
        DomainError error =
                new DomainError("booking.room_not_available", "Selected room is not available.");

        assertThat(error.code()).isEqualTo("booking.room_not_available");
        assertThat(error.getMessage()).isEqualTo("Selected room is not available.");
    }

    @Test
    void shouldDefaultToValidationCategory_whenCategoryNotSpecified() {
        DomainError error = new DomainError("sharedkernel.invalid_id", "Invalid identifier.");

        assertThat(error.category()).isEqualTo(DomainErrorCategory.VALIDATION);
    }

    @Test
    void shouldExposeDomainNeutralCategory_whenCreatedWithFactoryMethods() {
        assertThat(DomainError.notFound("booking.x_not_found", "Not found").category())
                .isEqualTo(DomainErrorCategory.NOT_FOUND);
        assertThat(DomainError.conflict("booking.room_not_available", "Conflict").category())
                .isEqualTo(DomainErrorCategory.CONFLICT);
        assertThat(DomainError.forbidden("booking.not_permitted", "Forbidden").category())
                .isEqualTo(DomainErrorCategory.FORBIDDEN);
        assertThat(DomainError.internal("booking.unexpected", "Internal").category())
                .isEqualTo(DomainErrorCategory.INTERNAL);
    }

    @Test
    void shouldRejectInvalidCodeFormat_whenCodeDoesNotMatchModuleSnakeCase() {
        assertThatThrownBy(() -> new DomainError("INVALID_CODE", "Invalid code format"))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> new DomainError("booking.RoomNotAvailable", "Invalid camelCase"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
