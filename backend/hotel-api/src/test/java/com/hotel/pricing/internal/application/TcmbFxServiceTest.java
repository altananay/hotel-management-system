package com.hotel.pricing.internal.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.hotel.pricing.api.SupportedCurrency;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

/** Unit tests for TCMB refresh fallback and last-published date handling (AC-3). */
class TcmbFxServiceTest {

    @Test
    void shouldStoreParsedXmlDate_whenWeekendFeedReturnsLastPublishedDate() throws IOException {
        TcmbFxStoreService store = mock(TcmbFxStoreService.class);
        String xml = fixture("tcmb-weekend.xml");
        TcmbFxService service = new TcmbFxService(() -> xml, new TcmbFxParser(), store);

        service.refresh();

        verify(store)
                .store(
                        argThat(
                                rates ->
                                        rates.rateDate().toString().equals("2026-11-06")
                                                && rates.sellingRates()
                                                        .containsKey(SupportedCurrency.EUR)));
    }

    @Test
    void shouldKeepLastKnownRates_whenTcmbIsUnreachable() {
        TcmbFxStoreService store = mock(TcmbFxStoreService.class);
        TcmbFxService service =
                new TcmbFxService(
                        () -> {
                            throw new TcmbUnavailableException("down", null);
                        },
                        new TcmbFxParser(),
                        store);

        service.refresh();

        verifyNoInteractions(store);
    }

    private static String fixture(String name) throws IOException {
        return new String(
                new ClassPathResource("pricing/" + name).getInputStream().readAllBytes(),
                StandardCharsets.UTF_8);
    }
}
