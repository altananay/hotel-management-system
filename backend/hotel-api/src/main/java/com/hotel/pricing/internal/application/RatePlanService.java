package com.hotel.pricing.internal.application;

import com.hotel.pricing.api.PricingDefaultIncompleteException;
import com.hotel.pricing.internal.domain.PriceRules;
import com.hotel.pricing.internal.domain.PricingErrorCodes;
import com.hotel.pricing.internal.domain.PricingIds;
import com.hotel.pricing.internal.domain.RatePlan;
import com.hotel.pricing.internal.persistence.RatePlanEntity;
import com.hotel.pricing.internal.persistence.RatePlanJpaRepository;
import com.hotel.sharedkernel.api.DomainError;
import com.hotel.sharedkernel.api.DomainErrorCategory;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RatePlanService {

    private final RatePlanJpaRepository repository;
    private final PriceMatrixService priceMatrixService;
    private final Clock clock;

    public RatePlanService(
            RatePlanJpaRepository repository,
            PriceMatrixService priceMatrixService,
            Clock clock) {
        this.repository = repository;
        this.priceMatrixService = priceMatrixService;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<RatePlan> list() {
        return repository.findAllOrdered().stream().map(RatePlanEntity::toDomain).toList();
    }

    @Transactional
    public RatePlan create(String code, String nameTr, String nameEn, boolean active) {
        String normalizedCode = PriceRules.requireCode(code);
        String normalizedNameTr = PriceRules.requireName(nameTr, "Turkish name");
        String normalizedNameEn = PriceRules.requireName(nameEn, "English name");
        if (repository.findByCode(normalizedCode).isPresent()) {
            throw duplicateCode();
        }
        Instant now = clock.instant();
        RatePlan ratePlan =
                RatePlan.create(
                        PricingIds.newId(), normalizedCode, normalizedNameTr, normalizedNameEn, active, now);
        try {
            return repository.saveAndFlush(RatePlanEntity.fromDomain(ratePlan)).toDomain();
        } catch (DataIntegrityViolationException ex) {
            throw duplicateCode();
        }
    }

    @Transactional
    public RatePlan update(UUID id, String nameTr, String nameEn, Boolean active) {
        RatePlanEntity entity = repository.findLockedById(id).orElseThrow(this::notFound);
        String newNameTr = nameTr == null ? entity.getNameTr() : PriceRules.requireName(nameTr, "Turkish name");
        String newNameEn = nameEn == null ? entity.getNameEn() : PriceRules.requireName(nameEn, "English name");
        boolean newActive = active == null ? entity.isActive() : active;
        if (!newActive && entity.isDefaultPlan()) {
            throw new DomainError(
                    PricingErrorCodes.RATE_PLAN_IN_USE,
                    "The default plan cannot be deactivated; make another active plan default first.",
                    DomainErrorCategory.CONFLICT);
        }
        entity.update(newNameTr, newNameEn, newActive, clock.instant());
        return repository.save(entity).toDomain();
    }

    @Transactional
    public RatePlan setDefault(UUID id) {
        List<RatePlanEntity> all = repository.findAllLocked();
        RatePlanEntity target =
                all.stream()
                        .filter(plan -> plan.getId().equals(id))
                        .findFirst()
                        .orElseThrow(this::notFound);
        if (!target.isActive()) {
            throw new DomainError(
                    PricingErrorCodes.RATE_PLAN_IN_USE,
                    "Only an active rate plan can be the default.",
                    DomainErrorCategory.CONFLICT);
        }
        var missing = priceMatrixService.findMissingForPlan(id);
        if (!missing.isEmpty()) {
            throw new PricingDefaultIncompleteException(missing);
        }
        Instant now = clock.instant();
        for (RatePlanEntity plan : all) {
            boolean shouldBeDefault = plan.getId().equals(id);
            if (plan.isDefaultPlan() != shouldBeDefault) {
                plan.setDefaultPlan(shouldBeDefault, now);
            }
        }
        try {
            repository.saveAll(all);
            return target.toDomain();
        } catch (DataIntegrityViolationException ex) {
            throw new DomainError(
                    PricingErrorCodes.RATE_PLAN_IN_USE,
                    "Another default plan was selected concurrently; refresh and retry.",
                    DomainErrorCategory.CONFLICT);
        }
    }

    @Transactional(readOnly = true)
    public RatePlan getRequired(UUID id) {
        return repository.findById(id).map(RatePlanEntity::toDomain).orElseThrow(this::notFound);
    }

    @Transactional(readOnly = true)
    public java.util.Optional<RatePlan> findActiveDefault() {
        return repository.findFirstByDefaultPlanTrue().filter(RatePlanEntity::isActive).map(RatePlanEntity::toDomain);
    }

    private DomainError notFound() {
        return new DomainError(
                PricingErrorCodes.RATE_PLAN_NOT_FOUND,
                "Rate plan was not found.",
                DomainErrorCategory.NOT_FOUND);
    }

    private DomainError duplicateCode() {
        return new DomainError(
                PricingErrorCodes.RATE_PLAN_DUPLICATE,
                "A rate plan with this code already exists.",
                DomainErrorCategory.CONFLICT);
    }
}
