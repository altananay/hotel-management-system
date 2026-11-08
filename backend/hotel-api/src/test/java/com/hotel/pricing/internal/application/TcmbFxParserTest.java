package com.hotel.pricing.internal.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.hotel.pricing.api.SupportedCurrency;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

/** Unit tests for TCMB today.xml parsing and date semantics (AC-3). */
class TcmbFxParserTest {

    private final TcmbFxParser parser = new TcmbFxParser();

    @Test
    void shouldParseSellingRatesAndRateDate_whenXmlIsValid() throws IOException {
        TcmbRates rates = parser.parse(fixture("tcmb-today.xml"));
        assertThat(rates.rateDate()).hasToString("2026-11-06");
        assertThat(rates.sellingRates())
                .containsEntry(SupportedCurrency.EUR, new java.math.BigDecimal("42.500000"))
                .containsEntry(SupportedCurrency.USD, new java.math.BigDecimal("39.000000"))
                .doesNotContainKey(SupportedCurrency.TRY);
    }

    @Test
    void shouldUseXmlDate_whenWeekendOrHolidayFeedReturnsLastPublishedDate() throws IOException {
        TcmbRates rates = parser.parse(fixture("tcmb-weekend.xml"));
        assertThat(rates.rateDate()).hasToString("2026-11-08");
        assertThat(rates.sellingRates()).containsKeys(SupportedCurrency.EUR, SupportedCurrency.USD);
    }

    @Test
    void shouldReturnNoRates_whenEurUsdSellingRateIsMissing() throws IOException {
        TcmbRates rates = parser.parse(fixture("tcmb-no-eurusd.xml"));
        assertThat(rates.sellingRates()).isEmpty();
    }

    @Test
    void shouldRejectBrokenXml() throws IOException {
        assertThatThrownBy(() -> parser.parse(fixture("tcmb-broken.xml")))
                .isInstanceOf(TcmbParseException.class);
    }

    @Test
    void shouldRejectEmptyXml() {
        assertThatThrownBy(() -> parser.parse(""))
                .isInstanceOf(TcmbParseException.class);
    }

    private static String fixture(String name) throws IOException {
        return new String(
                new ClassPathResource("pricing/" + name)
                        .getInputStream()
                        .readAllBytes(),
                StandardCharsets.UTF_8);
    }
}
