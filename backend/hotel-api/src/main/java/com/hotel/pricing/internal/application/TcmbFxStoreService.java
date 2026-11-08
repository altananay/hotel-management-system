package com.hotel.pricing.internal.application;

import com.hotel.pricing.api.FxSource;
import com.hotel.pricing.api.SupportedCurrency;
import com.hotel.pricing.internal.domain.FxRate;
import com.hotel.pricing.internal.domain.PricingIds;
import com.hotel.pricing.internal.persistence.FxRateEntity;
import com.hotel.pricing.internal.persistence.FxRateJpaRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Short write transaction for parsed TCMB rates; network I/O stays outside the transaction. */
@Service
public class TcmbFxStoreService {

    private final FxRateJpaRepository repository;
    private final Clock clock;

    public TcmbFxStoreService(FxRateJpaRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Transactional
    public void store(TcmbRates rates) {
        Instant now = clock.instant();
        for (Map.Entry<SupportedCurrency, BigDecimal> entry : rates.sellingRates().entrySet()) {
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
