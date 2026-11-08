package com.hotel.pricing.internal.web;

import com.hotel.pricing.api.PricingQuoteApi;
import com.hotel.pricing.api.QuoteRequest;
import com.hotel.pricing.internal.web.dto.QuoteResponse;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/pricing")
public class PublicPricingController {

    private final PricingQuoteApi pricingQuoteApi;
    private final PricingWebMapper mapper;

    public PublicPricingController(
            PricingQuoteApi pricingQuoteApi, PricingWebMapper mapper) {
        this.pricingQuoteApi = pricingQuoteApi;
        this.mapper = mapper;
    }

    @GetMapping("/quote")
    public QuoteResponse quote(
            @RequestParam UUID roomTypeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkIn,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkOut,
            @RequestParam int guests,
            @RequestParam String currency) {
        return mapper.toResponse(
                pricingQuoteApi.quote(
                        new QuoteRequest(roomTypeId, checkIn, checkOut, guests, currency)));
    }
}
