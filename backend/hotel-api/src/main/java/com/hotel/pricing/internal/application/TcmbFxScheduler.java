package com.hotel.pricing.internal.application;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
        prefix = "hotel.pricing.tcmb",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true)
public class TcmbFxScheduler {

    private final TcmbFxService service;

    public TcmbFxScheduler(TcmbFxService service) {
        this.service = service;
    }

    @Scheduled(
            cron = "${hotel.pricing.tcmb.cron:0 30 15 * * MON-FRI}",
            zone = "${hotel.pricing.tcmb.zone:Europe/Istanbul}")
    public void refresh() {
        service.refresh();
    }
}
