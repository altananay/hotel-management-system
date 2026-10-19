package com.hotel.sharedkernel.api;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;
import java.util.Objects;

public final class Money implements Comparable<Money> {

    public static final String CURRENCY_MISMATCH_CODE = "sharedkernel.currency_mismatch";

    private final BigDecimal amount;
    private final Currency currency;

    private Money(BigDecimal amount, Currency currency) {
        this.amount = Objects.requireNonNull(amount, "amount must not be null");
        this.currency = Objects.requireNonNull(currency, "currency must not be null");
    }

    public static Money of(BigDecimal amount, Currency currency) {
        return new Money(amount, currency);
    }

    public static Money of(BigDecimal amount, String currencyCode) {
        Objects.requireNonNull(currencyCode, "currencyCode must not be null");
        return new Money(amount, Currency.getInstance(currencyCode));
    }

    public static Money of(String amount, String currencyCode) {
        Objects.requireNonNull(amount, "amount must not be null");
        Objects.requireNonNull(currencyCode, "currencyCode must not be null");
        return new Money(new BigDecimal(amount), Currency.getInstance(currencyCode));
    }

    public static Money zero(Currency currency) {
        return new Money(BigDecimal.ZERO, currency);
    }

    public static Money zero(String currencyCode) {
        Objects.requireNonNull(currencyCode, "currencyCode must not be null");
        return new Money(BigDecimal.ZERO, Currency.getInstance(currencyCode));
    }

    public BigDecimal amount() {
        return amount;
    }

    public Currency currency() {
        return currency;
    }

    public Money plus(Money other) {
        requireSameCurrency(other);
        return new Money(this.amount.add(other.amount), this.currency);
    }

    public Money minus(Money other) {
        requireSameCurrency(other);
        return new Money(this.amount.subtract(other.amount), this.currency);
    }

    public Money multiply(BigDecimal factor) {
        Objects.requireNonNull(factor, "factor must not be null");
        return new Money(this.amount.multiply(factor), this.currency);
    }

    public Money round(int scale, RoundingMode roundingMode) {
        Objects.requireNonNull(roundingMode, "roundingMode must not be null");
        return new Money(this.amount.setScale(scale, roundingMode), this.currency);
    }

    public boolean isZero() {
        return this.amount.compareTo(BigDecimal.ZERO) == 0;
    }

    public boolean isPositive() {
        return this.amount.compareTo(BigDecimal.ZERO) > 0;
    }

    public boolean isNegative() {
        return this.amount.compareTo(BigDecimal.ZERO) < 0;
    }

    @Override
    public int compareTo(Money other) {
        requireSameCurrency(other);
        return this.amount.compareTo(other.amount);
    }

    private void requireSameCurrency(Money other) {
        Objects.requireNonNull(other, "other money must not be null");
        if (!this.currency.equals(other.currency)) {
            throw new DomainError(
                    CURRENCY_MISMATCH_CODE,
                    "Currency mismatch: cannot operate on "
                            + this.currency.getCurrencyCode()
                            + " and "
                            + other.currency.getCurrencyCode());
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Money other)) {
            return false;
        }
        return this.currency.equals(other.currency)
                && this.amount.compareTo(other.amount) == 0;
    }

    @Override
    public int hashCode() {
        BigDecimal normalized =
                this.amount.compareTo(BigDecimal.ZERO) == 0
                        ? BigDecimal.ZERO
                        : this.amount.stripTrailingZeros();
        return Objects.hash(normalized, currency);
    }

    @Override
    public String toString() {
        return amount.toPlainString() + " " + currency.getCurrencyCode();
    }
}
