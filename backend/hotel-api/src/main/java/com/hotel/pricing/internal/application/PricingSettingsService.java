package com.hotel.pricing.internal.application;

import com.hotel.pricing.internal.domain.PriceRules;
import com.hotel.pricing.internal.domain.PricingSettings;
import com.hotel.pricing.internal.persistence.PricingSettingsEntity;
import com.hotel.pricing.internal.persistence.PricingSettingsJpaRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PricingSettingsService {

    private static final short SETTINGS_ID = 1;

    private final PricingSettingsJpaRepository repository;
    private final PricingProperties properties;
    private final Clock clock;

    public PricingSettingsService(
            PricingSettingsJpaRepository repository,
            PricingProperties properties,
            Clock clock) {
        this.repository = repository;
        this.properties = properties;
        this.clock = clock;
    }

    @Transactional
    public PricingSettings get() {
        return loadOrCreate().toDomain();
    }

    @Transactional
    public PricingSettings initializeIfAbsent() {
        return loadOrCreate().toDomain();
    }

    @Transactional
    public PricingSettings update(BigDecimal bufferPercent, BigDecimal roundingStep) {
        BigDecimal validatedBuffer = PriceRules.requireBuffer(bufferPercent);
        BigDecimal validatedStep = PriceRules.requireRoundingStep(roundingStep);
        PricingSettingsEntity entity = loadOrCreate();
        entity.update(validatedBuffer, validatedStep, clock.instant());
        repository.save(entity);
        return entity.toDomain();
    }

    private PricingSettingsEntity loadOrCreate() {
        return repository
                .findById(SETTINGS_ID)
                .orElseGet(
                        () -> {
                            Instant now = clock.instant();
                            PricingSettingsEntity created =
                                    new PricingSettingsEntity(
                                            SETTINGS_ID,
                                            properties.getDefaults().getBufferPercent(),
                                            properties.getDefaults().getRoundingStep(),
                                            now,
                                            now);
                            try {
                                return repository.saveAndFlush(created);
                            } catch (DataIntegrityViolationException ex) {
                                return repository.findById(SETTINGS_ID).orElseThrow(() -> ex);
                            }
                        });
    }
}
