package com.hotel.pricing.api;

/**
 * Synchronous pricing port consumed by booking at reservation time. Both methods always
 * recalculate from current prices, FX and settings; pricing does not store quotes.
 */
public interface PricingQuoteApi {

    Quote quote(QuoteRequest request);

    PriceSnapshot snapshot(QuoteRequest request);
}
