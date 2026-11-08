package com.hotel.pricing.internal.persistence;

import com.hotel.pricing.api.FxSource;
import com.hotel.pricing.api.SupportedCurrency;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FxRateJpaRepository extends JpaRepository<FxRateEntity, java.util.UUID> {

    Optional<FxRateEntity> findByCurrencyAndSourceAndRateDate(
            SupportedCurrency currency, FxSource source, LocalDate rateDate);

    Optional<FxRateEntity> findTopByCurrencyAndSourceAndRateDateLessThanEqualOrderByRateDateDesc(
            SupportedCurrency currency, FxSource source, LocalDate rateDate);

    Optional<FxRateEntity> findTopByCurrencyAndSourceOrderByRateDateDesc(
            SupportedCurrency currency, FxSource source);

    List<FxRateEntity>
            findByCurrencyAndSourceAndRateDateLessThanEqualOrderByRateDateDesc(
                    SupportedCurrency currency, FxSource source, LocalDate rateDate);

    List<FxRateEntity> findByCurrencyAndSourceAndRateDateBetweenOrderByRateDateAsc(
            SupportedCurrency currency, FxSource source, LocalDate from, LocalDate to);

    Page<FxRateEntity> findByCurrencyOrderByRateDateDesc(
            SupportedCurrency currency, Pageable pageable);

    Page<FxRateEntity> findAllByOrderByRateDateDesc(Pageable pageable);

    Page<FxRateEntity> findBySourceOrderByRateDateDesc(FxSource source, Pageable pageable);

    Page<FxRateEntity> findBySourceAndCurrencyOrderByRateDateDesc(
            FxSource source, SupportedCurrency currency, Pageable pageable);

    Page<FxRateEntity> findBySourceAndRateDateBetweenOrderByRateDateDesc(
            FxSource source, LocalDate from, LocalDate to, Pageable pageable);

    Page<FxRateEntity> findBySourceAndCurrencyAndRateDateBetweenOrderByRateDateDesc(
            FxSource source,
            SupportedCurrency currency,
            LocalDate from,
            LocalDate to,
            Pageable pageable);

    Page<FxRateEntity> findByRateDateBetweenOrderByRateDateDesc(
            LocalDate from, LocalDate to, Pageable pageable);

    Page<FxRateEntity> findByCurrencyAndRateDateBetweenOrderByRateDateDesc(
            SupportedCurrency currency, LocalDate from, LocalDate to, Pageable pageable);
}
