package com.hotel.pricing.internal.web;

import com.hotel.pricing.internal.application.PricingAdminQueryService;
import com.hotel.pricing.internal.application.PricingAdminService;
import com.hotel.pricing.internal.web.dto.PriceMatrixRequest;
import com.hotel.pricing.internal.web.dto.PriceMatrixResponse;
import com.hotel.pricing.internal.web.dto.RatePlanCreateRequest;
import com.hotel.pricing.internal.web.dto.RatePlanResponse;
import com.hotel.pricing.internal.web.dto.RatePlanUpdateRequest;
import com.hotel.pricing.internal.web.dto.SeasonCreateRequest;
import com.hotel.pricing.internal.web.dto.SeasonResponse;
import com.hotel.pricing.internal.web.dto.SeasonUpdateRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin-api/pricing")
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'MANAGER')")
public class AdminRatePlanController {

    private final PricingAdminService adminService;
    private final PricingAdminQueryService queryService;

    public AdminRatePlanController(
            PricingAdminService adminService, PricingAdminQueryService queryService) {
        this.adminService = adminService;
        this.queryService = queryService;
    }

    @GetMapping("/rate-plans")
    public List<RatePlanResponse> ratePlans() {
        return queryService.listRatePlans();
    }

    @PostMapping("/rate-plans")
    public ResponseEntity<RatePlanResponse> createRatePlan(
            @Valid @RequestBody RatePlanCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(adminService.createRatePlan(request));
    }

    @PatchMapping("/rate-plans/{id}")
    public RatePlanResponse updateRatePlan(
            @PathVariable UUID id, @Valid @RequestBody RatePlanUpdateRequest request) {
        return adminService.updateRatePlan(id, request);
    }

    @PutMapping("/rate-plans/{id}/default")
    public RatePlanResponse setDefault(@PathVariable UUID id) {
        return adminService.setDefault(id);
    }

    @GetMapping("/rate-plans/{id}/seasons")
    public List<SeasonResponse> seasons(@PathVariable UUID id) {
        return queryService.listSeasons(id);
    }

    @PostMapping("/rate-plans/{id}/seasons")
    public ResponseEntity<SeasonResponse> createSeason(
            @PathVariable UUID id, @Valid @RequestBody SeasonCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(adminService.createSeason(id, request));
    }

    @PatchMapping("/seasons/{id}")
    public SeasonResponse updateSeason(
            @PathVariable UUID id, @Valid @RequestBody SeasonUpdateRequest request) {
        return adminService.updateSeason(id, request);
    }

    @DeleteMapping("/seasons/{id}")
    public ResponseEntity<Void> deleteSeason(@PathVariable UUID id) {
        adminService.deleteSeason(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/rate-plans/{id}/prices")
    public PriceMatrixResponse basePrices(@PathVariable UUID id) {
        return queryService.baseMatrix(id);
    }

    @PutMapping("/rate-plans/{id}/prices")
    public PriceMatrixResponse saveBasePrices(
            @PathVariable UUID id, @Valid @RequestBody PriceMatrixRequest request) {
        return adminService.saveBasePrices(id, request);
    }

    @GetMapping("/seasons/{id}/prices")
    public PriceMatrixResponse seasonPrices(@PathVariable UUID id) {
        return queryService.seasonMatrix(id);
    }

    @PutMapping("/seasons/{id}/prices")
    public PriceMatrixResponse saveSeasonPrices(
            @PathVariable UUID id, @Valid @RequestBody PriceMatrixRequest request) {
        return adminService.saveSeasonPrices(id, request);
    }
}
