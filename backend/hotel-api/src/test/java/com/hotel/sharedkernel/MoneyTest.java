package com.hotel.sharedkernel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.hotel.sharedkernel.api.DomainError;
import com.hotel.sharedkernel.api.Money;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.Currency;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link Money} (AC-4).
 */
class MoneyTest {

    private static final Currency TRY = Currency.getInstance("TRY");
    private static final Currency EUR = Currency.getInstance("EUR");

    @Test
    void shouldAddSubtractAndMultiply_whenSameCurrency() {
        Money price = Money.of("100.50", "TRY");
        Money discount = Money.of("20.25", "TRY");

        assertThat(price.plus(discount)).isEqualTo(Money.of("120.75", "TRY"));
        assertThat(price.minus(discount)).isEqualTo(Money.of("80.25", "TRY"));
        assertThat(price.multiply(new BigDecimal("1.02")))
                .isEqualTo(Money.of("102.5100", "TRY"));
    }

    @Test
    void shouldAllowZeroAndNegativeAmounts_whenCreatingOrOperating() {
        Money zero = Money.zero(TRY);
        Money negative = Money.of(new BigDecimal("-50.00"), TRY);

        assertThat(zero.isZero()).isTrue();
        assertThat(zero.isPositive()).isFalse();
        assertThat(zero.isNegative()).isFalse();
        assertThat(negative.isNegative()).isTrue();
        assertThat(negative.isPositive()).isFalse();
    }

    @Test
    void shouldRoundOnlyWhenExplicitlyRequested_whenCallingRound() {
        Money unrounded = Money.of("100.555", "EUR");

        assertThat(unrounded.amount().scale()).isEqualTo(3);

        Money roundedHalfUp = unrounded.round(2, RoundingMode.HALF_UP);
        assertThat(roundedHalfUp.amount()).isEqualByComparingTo("100.56");
        assertThat(roundedHalfUp.amount().scale()).isEqualTo(2);
    }

    @Test
    void shouldThrowCurrencyMismatchDomainError_whenOperatingOnDifferentCurrencies() {
        Money tryMoney = Money.of("100.00", "TRY");
        Money eurMoney = Money.of("100.00", "EUR");

        assertThatThrownBy(() -> tryMoney.plus(eurMoney))
                .isInstanceOf(DomainError.class)
                .extracting(ex -> ((DomainError) ex).code())
                .isEqualTo(Money.CURRENCY_MISMATCH_CODE);

        assertThatThrownBy(() -> tryMoney.minus(eurMoney))
                .isInstanceOf(DomainError.class)
                .extracting(ex -> ((DomainError) ex).code())
                .isEqualTo(Money.CURRENCY_MISMATCH_CODE);

        assertThatThrownBy(() -> tryMoney.compareTo(eurMoney))
                .isInstanceOf(DomainError.class)
                .extracting(ex -> ((DomainError) ex).code())
                .isEqualTo(Money.CURRENCY_MISMATCH_CODE);
    }

    @Test
    void shouldCompareScaleIndependentlyInEqualsAndReturnFalseForDifferentCurrency_whenCheckingEquality() {
        Money tenNoScale = Money.of("10", "TRY");
        Money tenTwoDecimals = Money.of("10.00", "TRY");
        Money tenEur = Money.of("10.00", "EUR");

        assertThat(tenNoScale).isEqualTo(tenTwoDecimals);
        assertThat(tenNoScale.hashCode()).isEqualTo(tenTwoDecimals.hashCode());
        assertThat(tenTwoDecimals).isNotEqualTo(tenEur);
        assertThat(Money.of("0", "TRY")).isEqualTo(Money.of("0.00", "TRY"));
        assertThat(Money.of("0", "TRY").hashCode())
                .isEqualTo(Money.of("0.00", "TRY").hashCode());
    }

    @Test
    void shouldNotExposeDivideOrFloatingPointOverloads_whenInspectingMoneyApi() {
        boolean hasDivide =
                Arrays.stream(Money.class.getDeclaredMethods())
                        .map(Method::getName)
                        .anyMatch(name -> name.startsWith("divide"));
        assertThat(hasDivide).isFalse();

        boolean hasFloatingPointMethod =
                Arrays.stream(Money.class.getDeclaredMethods())
                        .flatMap(m -> Arrays.stream(m.getParameterTypes()))
                        .anyMatch(
                                t ->
                                        t == double.class
                                                || t == float.class
                                                || t == Double.class
                                                || t == Float.class);
        assertThat(hasFloatingPointMethod).isFalse();

        boolean hasFloatingPointConstructor =
                Arrays.stream(Money.class.getDeclaredConstructors())
                        .map(Constructor::getParameterTypes)
                        .flatMap(Arrays::stream)
                        .anyMatch(
                                t ->
                                        t == double.class
                                                || t == float.class
                                                || t == Double.class
                                                || t == Float.class);
        assertThat(hasFloatingPointConstructor).isFalse();
    }
}
