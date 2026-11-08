package com.hotel.pricing.internal.application;

import com.hotel.pricing.api.FxSource;
import com.hotel.pricing.api.SupportedCurrency;
import com.hotel.pricing.internal.domain.FxRate;
import com.hotel.pricing.internal.domain.PriceRules;
import com.hotel.pricing.internal.domain.PricingErrorCodes;
import com.hotel.pricing.internal.domain.PricingIds;
import com.hotel.pricing.internal.persistence.FxRateEntity;
import com.hotel.pricing.internal.persistence.FxRateJpaRepository;
import com.hotel.sharedkernel.api.DomainError;
import com.hotel.sharedkernel.api.DomainErrorCategory;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ManualFxRateService {

    private static final int MAX_PAGE_SIZE = 100;

    private final FxRateJpaRepository repository;
    private final Clock clock;

    public ManualFxRateService(FxRateJpaRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public Page<FxRate> list(
            String currencyRaw, LocalDate from, LocalDate to, int page, int size) {
        validatePage(page, size);
        SupportedCurrency currency =
                currencyRaw == null || currencyRaw.isBlank()
                        ? null
                        : SupportedCurrency.from(currencyRaw);
        PageRequest pageable = PageRequest.of(page, size);
        Page<FxRateEntity> result;
        if (currency == null && from == null) {
            result = repository.findBySourceOrderByRateDateDesc(FxSource.MANUAL, pageable);
        } else if (currency == null) {
            result =
                    repository.findBySourceAndRateDateBetweenOrderByRateDateDesc(
                            FxSource.MANUAL, from, to == null ? LocalDate.now(clock) : to, pageable);
        } else if (from == null) {
            result =
                    repository.findBySourceAndCurrencyOrderByRateDateDesc(
                            FxSource.MANUAL, currency, pageable);
        } else {
            result =
                    repository.findBySourceAndCurrencyAndRateDateBetweenOrderByRateDateDesc(
                            FxSource.MANUAL, currency, from, to == null ? LocalDate.now(clock) : to, pageable);
        }
        return result.map(FxRateEntity::toDomain);
    }

    @Transactional
    public FxRate create(String currencyRaw, LocalDate rateDate, BigDecimal rate) {
        SupportedCurrency currency = requireFxCurrency(currencyRaw);
        if (rateDate == null) {
            throw PriceRules.invalid("Rate date is required.");
        }
        BigDecimal validatedRate = PriceRules.requireFxRate(rate);
        if (repository
                .findByCurrencyAndSourceAndRateDate(currency, FxSource.MANUAL, rateDate)
                .isPresent()) {
            throw duplicate();
        }
        Instant now = clock.instant();
        FxRate fxRate =
                FxRate.create(
                        PricingIds.newId(),
                        currency,
                        FxSource.MANUAL,
                        rateDate,
                        validatedRate,
                        now,
                        now);
        return repository.saveAndFlush(FxRateEntity.fromDomain(fxRate)).toDomain();
    }

    @Transactional
    public FxRate update(UUID id, BigDecimal rate) {
        FxRateEntity entity = repository.findById(id).orElseThrow(this::notFound);
        requireManual(entity);
        BigDecimal validatedRate = PriceRules.requireFxRate(rate);
        Instant now = clock.instant();
        entity.updateRate(validatedRate, now, now);
        return repository.save(entity).toDomain();
    }

    @Transactional
    public void delete(UUID id) {
        FxRateEntity entity = repository.findById(id).orElseThrow(this::notFound);
        requireManual(entity);
        repository.delete(entity);
    }

    private SupportedCurrency requireFxCurrency(String currencyRaw) {
        SupportedCurrency currency = SupportedCurrency.from(currencyRaw);
        if (currency == SupportedCurrency.TRY) {
            throw new DomainError(
                    PricingErrorCodes.INVALID_CURRENCY,
                    "Manual FX rates are supported only for EUR and USD.",
                    DomainErrorCategory.VALIDATION);
        }
        return currency;
    }

    private void requireManual(FxRateEntity entity) {
        if (entity.getSource() != FxSource.MANUAL) {
            throw PriceRules.invalid("Only manual FX rates can be changed or deleted.");
        }
    }

    private void validatePage(int page, int size) {
        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
            throw PriceRules.invalid("Page must be non-negative and size must be between 1 and 100.");
        }
    }

    private DomainError notFound() {
        return new DomainError(
                PricingErrorCodes.MANUAL_FX_NOT_FOUND,
                "Manual FX rate was not found.",
                DomainErrorCategory.NOT_FOUND);
    }

    private DomainError duplicate() {
        return new DomainError(
                PricingErrorCodes.MANUAL_FX_DUPLICATE,
                "A manual FX rate already exists for this currency and date.",
                DomainErrorCategory.CONFLICT);
    }
}
