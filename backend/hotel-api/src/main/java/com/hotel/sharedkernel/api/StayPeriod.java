package com.hotel.sharedkernel.api;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

public final class StayPeriod {

    public static final String INVALID_STAY_PERIOD_CODE = "sharedkernel.invalid_stay_period";

    private final LocalDate checkIn;
    private final LocalDate checkOut;

    private StayPeriod(LocalDate checkIn, LocalDate checkOut) {
        Objects.requireNonNull(checkIn, "checkIn must not be null");
        Objects.requireNonNull(checkOut, "checkOut must not be null");
        if (!checkOut.isAfter(checkIn)) {
            throw new DomainError(
                    INVALID_STAY_PERIOD_CODE,
                    "checkOut (" + checkOut + ") must be strictly after checkIn (" + checkIn + ")");
        }
        this.checkIn = checkIn;
        this.checkOut = checkOut;
    }

    public static StayPeriod of(LocalDate checkIn, LocalDate checkOut) {
        return new StayPeriod(checkIn, checkOut);
    }

    public static StayPeriod of(HotelDate checkIn, HotelDate checkOut) {
        Objects.requireNonNull(checkIn, "checkIn must not be null");
        Objects.requireNonNull(checkOut, "checkOut must not be null");
        return new StayPeriod(checkIn.value(), checkOut.value());
    }

    public LocalDate checkIn() {
        return checkIn;
    }

    public LocalDate checkOut() {
        return checkOut;
    }

    public long nights() {
        return ChronoUnit.DAYS.between(checkIn, checkOut);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof StayPeriod other)) {
            return false;
        }
        return checkIn.equals(other.checkIn) && checkOut.equals(other.checkOut);
    }

    @Override
    public int hashCode() {
        return Objects.hash(checkIn, checkOut);
    }

    @Override
    public String toString() {
        return checkIn + "/" + checkOut + " (" + nights() + " nights)";
    }
}
