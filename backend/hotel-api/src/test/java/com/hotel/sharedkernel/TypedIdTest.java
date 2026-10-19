package com.hotel.sharedkernel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.hotel.sharedkernel.api.DomainError;
import com.hotel.sharedkernel.api.TypedId;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link TypedId} (AC-4).
 */
class TypedIdTest {

    private static final class SampleRoomId extends TypedId {
        private SampleRoomId(UUID value) {
            super(value);
        }

        static SampleRoomId generate() {
            return new SampleRoomId(newUuidV7());
        }

        static SampleRoomId of(String raw) {
            return new SampleRoomId(parseUuid(raw));
        }
    }

    private static final class SampleBookingId extends TypedId {
        private SampleBookingId(UUID value) {
            super(value);
        }

        static SampleBookingId of(UUID value) {
            return new SampleBookingId(value);
        }
    }

    @Test
    void shouldGenerateVersion7UuidsInStrictlyIncreasingOrder_whenGeneratedWithinSameMillisecond() {
        List<SampleRoomId> generated = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            generated.add(SampleRoomId.generate());
        }

        for (int i = 0; i < generated.size(); i++) {
            assertThat(generated.get(i).value().version()).isEqualTo(7);
            if (i > 0) {
                assertThat(generated.get(i).toString())
                        .isGreaterThan(generated.get(i - 1).toString());
                assertThat(generated.get(i).compareTo(generated.get(i - 1))).isPositive();
            }
        }
    }

    @Test
    void shouldAcceptValidUuidFormatAndThrowInvalidIdOnMalformedString_whenParsingIncomingId() {
        String validV4 = "550e8400-e29b-41d4-a716-446655440000";
        SampleRoomId parsed = SampleRoomId.of(validV4);
        assertThat(parsed.toString()).isEqualTo(validV4);

        assertThatThrownBy(() -> SampleRoomId.of("not-a-uuid"))
                .isInstanceOf(DomainError.class)
                .extracting(ex -> ((DomainError) ex).code())
                .isEqualTo(TypedId.INVALID_ID_CODE);

        assertThatThrownBy(() -> SampleRoomId.of(""))
                .isInstanceOf(DomainError.class)
                .extracting(ex -> ((DomainError) ex).code())
                .isEqualTo(TypedId.INVALID_ID_CODE);

        assertThatThrownBy(() -> SampleRoomId.of(null))
                .isInstanceOf(DomainError.class)
                .extracting(ex -> ((DomainError) ex).code())
                .isEqualTo(TypedId.INVALID_ID_CODE);
    }

    @Test
    void shouldRequireSameConcreteSubtypeAndUuidForEquality_whenComparingTypedIds() {
        SampleRoomId roomId1 = SampleRoomId.generate();
        SampleRoomId roomId2 = SampleRoomId.of(roomId1.toString());
        SampleBookingId bookingIdWithSameUuid = SampleBookingId.of(roomId1.value());

        assertThat(roomId1).isEqualTo(roomId2);
        assertThat(roomId1.hashCode()).isEqualTo(roomId2.hashCode());
        assertThat((Object) roomId1).isNotEqualTo(bookingIdWithSameUuid);
    }
}
