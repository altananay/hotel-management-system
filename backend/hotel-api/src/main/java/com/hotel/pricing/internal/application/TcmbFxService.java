package com.hotel.pricing.internal.application;

import com.hotel.pricing.api.FxSource;
import com.hotel.pricing.internal.domain.FxRate;
import com.hotel.pricing.internal.domain.PricingIds;
import com.hotel.pricing.internal.persistence.FxRateEntity;
import com.hotel.pricing.internal.persistence.FxRateJpaRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TcmbFxService {

    private static final Logger log = LoggerFactory.getLogger(TcmbFxService.class);

    private final TcmbFxClient client;
    private final TcmbFxParser parser;
    private final FxRateJpaRepository repository;
    private final Clock clock;

    public TcmbFxService(
            TcmbFxClient client,
            TcmbFxParser parser,
            FxRateJpaRepository repository,
            Clock clock) {
        this.client = client;
        this.parser = parser;
        this.repository = repository;
        this.clock = clock;
    }

    @Transactional
    public void refresh() {
        String xml;
        try {
            xml = client.fetchTodayXml();
        } catch (RuntimeException ex) {
            log.warn("TCMB fetch failed; keeping the last known FX rates.", ex);
            return;
        }

        TcmbRates rates;
        try {
            rates = parser.parse(xml);
        } catch (RuntimeException ex) {
            log.warn("TCMB XML parse failed; keeping the last known FX rates.", ex);
            return;
        }

        Instant now = clock.instant();
        for (Map.Entry<com.hotel.pricing.api.SupportedCurrency, BigDecimal> entry :
                rates.sellingRates().entrySet()) {
            FxRateEntity entity =
                    repository
                            .findByCurrencyAndSourceAndRateDate(
                                    entry.getKey(), FxSource.TCMB, rates.rateDate())
                            .orElse(null);
            if (entity == null) {
                FxRate fxRate =
                        FxRate.create(
                                PricingIds.newId(),
                                entry.getKey(),
                                FxSource.TCMB,
                                rates.rateDate(),
                                entry.getValue(),
                                now,
                                now);
                repository.save(FxRateEntity.fromDomain(fxRate));
            } else {
                entity.updateRate(entry.getValue(), now, now);
                repository.save(entity);
            }
        }
    }
}
