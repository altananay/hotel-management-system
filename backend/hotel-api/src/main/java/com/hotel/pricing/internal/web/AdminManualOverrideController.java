package com.hotel.pricing.internal.web;

import com.hotel.pricing.internal.application.PricingAdminQueryService;
import com.hotel.pricing.internal.application.PricingAdminService;
import com.hotel.pricing.internal.domain.FxRate;
import com.hotel.pricing.internal.web.dto.ManualFxRateCreateRequest;
import com.hotel.pricing.internal.web.dto.ManualFxRateResponse;
import com.hotel.pricing.internal.web.dto.ManualFxRateUpdateRequest;
import com.hotel.pricing.internal.web.dto.ManualPriceCreateRequest;
import com.hotel.pricing.internal.web.dto.ManualPriceResponse;
import com.hotel.pricing.internal.web.dto.ManualPriceUpdateRequest;
import com.hotel.pricing.internal.web.dto.PageResponse;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin-api/pricing")
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'MANAGER')")
public class AdminManualOverrideController {

    private final PricingAdminService adminService;
    private final PricingAdminQueryService queryService;
    private final PricingWebMapper mapper;

    public AdminManualOverrideController(
            PricingAdminService adminService,
            PricingAdminQueryService queryService,
            PricingWebMapper mapper) {
        this.adminService = adminService;
        this.queryService = queryService;
        this.mapper = mapper;
    }

    @GetMapping("/manual-prices")
    public List<ManualPriceResponse> manualPrices(@RequestParam UUID ratePlanId) {
        return queryService.listManualPrices(ratePlanId).stream()
                .map(mapper::toResponse)
                .toList();
    }

    @PostMapping("/manual-prices")
    public ResponseEntity<ManualPriceResponse> createManualPrice(
            @Valid @RequestBody ManualPriceCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(
                        mapper.toResponse(
                                adminService.createManualPrice(
                                        request.ratePlanId(),
                                        request.roomTypeId(),
                                        request.guests(),
                                        request.startDate(),
                                        request.endDate(),
                                        request.amountTry())));
    }

    @PatchMapping("/manual-prices/{id}")
    public ManualPriceResponse updateManualPrice(
            @PathVariable UUID id, @Valid @RequestBody ManualPriceUpdateRequest request) {
        return mapper.toResponse(
                adminService.updateManualPrice(
                        id,
                        request.roomTypeId(),
                        request.guests(),
                        request.startDate(),
                        request.endDate(),
                        request.amountTry()));
    }

    @DeleteMapping("/manual-prices/{id}")
    public ResponseEntity<Void> deleteManualPrice(@PathVariable UUID id) {
        adminService.deleteManualPrice(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/manual-fx-rates")
    public PageResponse<ManualFxRateResponse> manualFxRates(
            @RequestParam(required = false) String currency,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<FxRate> result =
                queryService.listManualFxRates(currency, from, to, page, size);
        return new PageResponse<>(
                result.getContent().stream().map(mapper::toResponse).toList(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements());
    }

    @PostMapping("/manual-fx-rates")
    public ResponseEntity<ManualFxRateResponse> createManualFxRate(
            @Valid @RequestBody ManualFxRateCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(
                        mapper.toResponse(
                                adminService.createManualFxRate(
                                        request.currency(),
                                        request.rateDate(),
                                        request.rate())));
    }

    @PatchMapping("/manual-fx-rates/{id}")
    public ManualFxRateResponse updateManualFxRate(
            @PathVariable UUID id, @Valid @RequestBody ManualFxRateUpdateRequest request) {
        return mapper.toResponse(adminService.updateManualFxRate(id, request.rate()));
    }

    @DeleteMapping("/manual-fx-rates/{id}")
    public ResponseEntity<Void> deleteManualFxRate(@PathVariable UUID id) {
        adminService.deleteManualFxRate(id);
        return ResponseEntity.noContent().build();
    }
}
