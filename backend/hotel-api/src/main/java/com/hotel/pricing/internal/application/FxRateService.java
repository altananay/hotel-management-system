package com.hotel.pricing.internal.application;

import com.hotel.pricing.api.FxSource;
import com.hotel.pricing.api.SupportedCurrency;
import com.hotel.pricing.internal.domain.FxLookup;
import com.hotel.pricing.internal.domain.FxRate;
import com.hotel.pricing.internal.persistence.FxRateEntity;
import com.hotel.pricing.internal.persistence.FxRateJpaRepository;
import com.hotel.sharedkernel.api.HotelDate;
import java.time.Clock;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FxRateService {

    private static final int MAX_PAGE_SIZE = 100;

    private final FxRateJpaRepository repository;
    private final Clock clock;

    public FxRateService(FxRateJpaRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public Optional<FxRate> resolve(SupportedCurrency currency, LocalDate date) {
        if (currency == SupportedCurrency.TRY) {
            return Optional.empty();
        }
        Optional<FxRateEntity> manual =
                repository.findByCurrencyAndSourceAndRateDate(currency, FxSource.MANUAL, date);
        if (manual.isPresent()) {
            return manual.map(FxRateEntity::toDomain);
        }
        return repository
                .findTopByCurrencyAndSourceAndRateDateLessThanEqualOrderByRateDateDesc(
                        currency, FxSource.TCMB, date)
                .map(FxRateEntity::toDomain);
    }

    @Transactional(readOnly = true)
    public FxLookup lookup(SupportedCurrency currency, LocalDate from, LocalDate to) {
        if (currency == SupportedCurrency.TRY) {
            return (requestedCurrency, date) -> Optional.empty();
        }
        Map<LocalDate, FxRate> manualByDate = new HashMap<>();
        for (FxRateEntity entity :
                repository.findByCurrencyAndSourceAndRateDateBetweenOrderByRateDateAsc(
                        currency, FxSource.MANUAL, from, to)) {
            manualByDate.put(entity.getRateDate(), entity.toDomain());
        }
        List<FxRate> tcmbRates =
                repository
                        .findByCurrencyAndSourceAndRateDateLessThanEqualOrderByRateDateDesc(
                                currency, FxSource.TCMB, to)
                        .stream()
                        .map(FxRateEntity::toDomain)
                        .toList();
        return (requestedCurrency, date) -> {
            FxRate manual = manualByDate.get(date);
            if (manual != null) {
                return Optional.of(manual);
            }
            return tcmbRates.stream()
                    .filter(rate -> !rate.rateDate().isAfter(date))
                    .findFirst();
        };
    }

    @Transactional(readOnly = true)
    public Optional<FxRate> latestTcmb(SupportedCurrency currency) {
        return repository
                .findTopByCurrencyAndSourceOrderByRateDateDesc(currency, FxSource.TCMB)
                .map(FxRateEntity::toDomain);
    }

    @Transactional(readOnly = true)
    public Optional<FxRate> latestManual(SupportedCurrency currency) {
        return repository
                .findTopByCurrencyAndSourceOrderByRateDateDesc(currency, FxSource.MANUAL)
                .map(FxRateEntity::toDomain);
    }

    @Transactional(readOnly = true)
    public Page<FxRate> page(
            FxSource source,
            String currencyRaw,
            LocalDate from,
            LocalDate to,
            int page,
            int size) {
        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
            throw com.hotel.pricing.internal.domain.PriceRules.invalid(
                    "Page must be non-negative and size must be between 1 and 100.");
        }
        SupportedCurrency currency =
                currencyRaw == null || currencyRaw.isBlank()
                        ? null
                        : SupportedCurrency.from(currencyRaw);
        PageRequest pageable = PageRequest.of(page, size);
        LocalDate effectiveFrom = from == null ? LocalDate.of(1900, 1, 1) : from;
        LocalDate effectiveTo = to == null ? HotelDate.now(clock).value() : to;
        if (effectiveFrom.isAfter(effectiveTo)) {
            throw com.hotel.pricing.internal.domain.PriceRules.invalid("from must not be after to.");
        }
        Page<FxRateEntity> result;
        if (currency == null && from == null && to == null) {
            result = repository.findBySourceOrderByRateDateDesc(source, pageable);
        } else if (currency == null) {
            result =
                    repository.findBySourceAndRateDateBetweenOrderByRateDateDesc(
                            source, effectiveFrom, effectiveTo, pageable);
        } else if (from == null && to == null) {
            result =
                    repository.findBySourceAndCurrencyOrderByRateDateDesc(
                            source, currency, pageable);
        } else {
            result =
                    repository.findBySourceAndCurrencyAndRateDateBetweenOrderByRateDateDesc(
                            source, currency, effectiveFrom, effectiveTo, pageable);
        }
        return result.map(FxRateEntity::toDomain);
    }

    @Transactional(readOnly = true)
    public Page<FxRate> pageAll(
            String currencyRaw, LocalDate from, LocalDate to, int page, int size) {
        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
            throw com.hotel.pricing.internal.domain.PriceRules.invalid(
                    "Page must be non-negative and size must be between 1 and 100.");
        }
        SupportedCurrency currency =
                currencyRaw == null || currencyRaw.isBlank()
                        ? null
                        : SupportedCurrency.from(currencyRaw);
        PageRequest pageable = PageRequest.of(page, size);
        LocalDate effectiveFrom = from == null ? LocalDate.of(1900, 1, 1) : from;
        LocalDate effectiveTo = to == null ? HotelDate.now(clock).value() : to;
        if (effectiveFrom.isAfter(effectiveTo)) {
            throw com.hotel.pricing.internal.domain.PriceRules.invalid("from must not be after to.");
        }
        Page<FxRateEntity> result;
        if (currency == null && from == null && to == null) {
            result = repository.findAllByOrderByRateDateDesc(pageable);
        } else if (currency == null) {
            result =
                    repository.findByRateDateBetweenOrderByRateDateDesc(
                            effectiveFrom, effectiveTo, pageable);
        } else if (from == null && to == null) {
            result = repository.findByCurrencyOrderByRateDateDesc(currency, pageable);
        } else {
            result =
                    repository.findByCurrencyAndRateDateBetweenOrderByRateDateDesc(
                            currency, effectiveFrom, effectiveTo, pageable);
        }
        return result.map(FxRateEntity::toDomain);
    }

    public boolean isStale(FxRate fxRate) {
        return fxRate.isStale(HotelDate.now(clock).value());
    }
}
