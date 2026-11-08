package com.hotel.pricing.internal.application;

import com.hotel.pricing.internal.domain.PriceRules;
import com.hotel.pricing.internal.domain.PricingErrorCodes;
import com.hotel.pricing.internal.domain.PricingIds;
import com.hotel.pricing.internal.domain.Season;
import com.hotel.pricing.internal.persistence.RatePlanJpaRepository;
import com.hotel.pricing.internal.persistence.SeasonEntity;
import com.hotel.pricing.internal.persistence.SeasonJpaRepository;
import com.hotel.sharedkernel.api.DomainError;
import com.hotel.sharedkernel.api.DomainErrorCategory;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SeasonService {

    private final SeasonJpaRepository seasonRepository;
    private final RatePlanJpaRepository ratePlanRepository;
    private final Clock clock;

    public SeasonService(
            SeasonJpaRepository seasonRepository,
            RatePlanJpaRepository ratePlanRepository,
            Clock clock) {
        this.seasonRepository = seasonRepository;
        this.ratePlanRepository = ratePlanRepository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<Season> list(UUID ratePlanId) {
        requirePlan(ratePlanId);
        return seasonRepository.findByRatePlanIdOrderByStartDateAsc(ratePlanId).stream()
                .map(SeasonEntity::toDomain)
                .toList();
    }

    @Transactional
    public Season create(
            UUID ratePlanId,
            String code,
            String nameTr,
            String nameEn,
            LocalDate startDate,
            LocalDate endDate) {
        ratePlanRepository.findLockedById(ratePlanId).orElseThrow(this::planNotFound);
        String normalizedCode = PriceRules.requireCode(code);
        String normalizedNameTr = PriceRules.requireName(nameTr, "Turkish name");
        String normalizedNameEn = PriceRules.requireName(nameEn, "English name");
        PriceRules.requirePeriod(startDate, endDate);
        if (seasonRepository.existsByRatePlanIdAndCode(ratePlanId, normalizedCode)) {
            throw duplicateCode();
        }
        if (seasonRepository.existsOverlapping(ratePlanId, startDate, endDate)) {
            throw overlap();
        }
        Instant now = clock.instant();
        Season season =
                Season.create(
                        PricingIds.newId(),
                        ratePlanId,
                        normalizedCode,
                        normalizedNameTr,
                        normalizedNameEn,
                        startDate,
                        endDate,
                        now);
        try {
            return seasonRepository.saveAndFlush(SeasonEntity.fromDomain(season)).toDomain();
        } catch (DataIntegrityViolationException ex) {
            throw overlap();
        }
    }

    @Transactional
    public Season update(
            UUID seasonId,
            String nameTr,
            String nameEn,
            LocalDate startDate,
            LocalDate endDate) {
        SeasonEntity entity = seasonRepository.findById(seasonId).orElseThrow(this::notFound);
        ratePlanRepository.findLockedById(entity.getRatePlanId()).orElseThrow(this::planNotFound);
        String newNameTr =
                nameTr == null
                        ? entity.getNameTr()
                        : PriceRules.requireName(nameTr, "Turkish name");
        String newNameEn =
                nameEn == null
                        ? entity.getNameEn()
                        : PriceRules.requireName(nameEn, "English name");
        LocalDate newStartDate = startDate == null ? entity.getStartDate() : startDate;
        LocalDate newEndDate = endDate == null ? entity.getEndDate() : endDate;
        PriceRules.requirePeriod(newStartDate, newEndDate);
        if (seasonRepository.existsOverlappingExcluding(
                entity.getRatePlanId(), seasonId, newStartDate, newEndDate)) {
            throw overlap();
        }
        entity.update(newNameTr, newNameEn, newStartDate, newEndDate, clock.instant());
        return seasonRepository.save(entity).toDomain();
    }

    @Transactional
    public void delete(UUID seasonId) {
        SeasonEntity entity = seasonRepository.findById(seasonId).orElseThrow(this::notFound);
        ratePlanRepository.findLockedById(entity.getRatePlanId()).orElseThrow(this::planNotFound);
        seasonRepository.delete(entity);
    }

    @Transactional(readOnly = true)
    public Season getRequired(UUID seasonId) {
        return seasonRepository.findById(seasonId).map(SeasonEntity::toDomain).orElseThrow(this::notFound);
    }

    @Transactional(readOnly = true)
    public List<Season> findOverlapping(UUID ratePlanId, LocalDate startDate, LocalDate endDate) {
        return seasonRepository.findOverlapping(ratePlanId, startDate, endDate).stream()
                .map(SeasonEntity::toDomain)
                .toList();
    }

    private void requirePlan(UUID ratePlanId) {
        if (!ratePlanRepository.existsById(ratePlanId)) {
            throw planNotFound();
        }
    }

    private DomainError planNotFound() {
        return new DomainError(
                PricingErrorCodes.RATE_PLAN_NOT_FOUND,
                "Rate plan was not found.",
                DomainErrorCategory.NOT_FOUND);
    }

    private DomainError notFound() {
        return new DomainError(
                PricingErrorCodes.SEASON_NOT_FOUND,
                "Season was not found.",
                DomainErrorCategory.NOT_FOUND);
    }

    private DomainError duplicateCode() {
        return new DomainError(
                PricingErrorCodes.SEASON_DUPLICATE,
                "A season with this code already exists in the rate plan.",
                DomainErrorCategory.CONFLICT);
    }

    private DomainError overlap() {
        return new DomainError(
                PricingErrorCodes.SEASON_OVERLAP,
                "The season dates overlap another season in the same rate plan.",
                DomainErrorCategory.CONFLICT);
    }
}
