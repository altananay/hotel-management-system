package com.hotel.sharedkernel.api;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

public final class HotelDate implements Comparable<HotelDate> {

    public static final ZoneId ISTANBUL_ZONE = ZoneId.of("Europe/Istanbul");

    private final LocalDate value;

    private HotelDate(LocalDate value) {
        this.value = Objects.requireNonNull(value, "value must not be null");
    }

    public static HotelDate now(Clock clock) {
        Objects.requireNonNull(clock, "clock must not be null");
        return new HotelDate(LocalDate.now(clock.withZone(ISTANBUL_ZONE)));
    }

    public static HotelDate of(LocalDate date) {
        return new HotelDate(date);
    }

    public static HotelDate of(int year, int month, int dayOfMonth) {
        return new HotelDate(LocalDate.of(year, month, dayOfMonth));
    }

    public static HotelDate parse(String isoDate) {
        Objects.requireNonNull(isoDate, "isoDate must not be null");
        return new HotelDate(LocalDate.parse(isoDate, DateTimeFormatter.ISO_LOCAL_DATE));
    }

    public LocalDate value() {
        return value;
    }

    public boolean isBefore(HotelDate other) {
        Objects.requireNonNull(other, "other must not be null");
        return this.value.isBefore(other.value);
    }

    public boolean isAfter(HotelDate other) {
        Objects.requireNonNull(other, "other must not be null");
        return this.value.isAfter(other.value);
    }

    @Override
    public int compareTo(HotelDate other) {
        Objects.requireNonNull(other, "other must not be null");
        return this.value.compareTo(other.value);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof HotelDate other)) {
            return false;
        }
        return this.value.equals(other.value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }

    @Override
    public String toString() {
        return value.format(DateTimeFormatter.ISO_LOCAL_DATE);
    }
}
