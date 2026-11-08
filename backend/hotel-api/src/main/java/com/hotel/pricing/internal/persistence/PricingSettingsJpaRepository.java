package com.hotel.pricing.internal.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PricingSettingsJpaRepository
        extends JpaRepository<PricingSettingsEntity, Short> {}
