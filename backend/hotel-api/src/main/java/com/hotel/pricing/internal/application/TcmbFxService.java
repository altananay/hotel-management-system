package com.hotel.pricing.internal.application;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class TcmbFxService {

    private static final Logger log = LoggerFactory.getLogger(TcmbFxService.class);

    private final TcmbFxClient client;
    private final TcmbFxParser parser;
    private final TcmbFxStoreService storeService;

    public TcmbFxService(
            TcmbFxClient client, TcmbFxParser parser, TcmbFxStoreService storeService) {
        this.client = client;
        this.parser = parser;
        this.storeService = storeService;
    }

    /**
     * Network I/O and XML parsing deliberately happen outside a database transaction; only the
     * short upsert phase is transactional.
     */
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

        storeService.store(rates);
    }
}
