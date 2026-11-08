package com.hotel.pricing.internal.web;

import com.hotel.pricing.internal.application.PriceMatrixService;
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
import com.hotel.pricing.internal.web.dto.SetDefaultRatePlanRequest;
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
    private final PricingWebMapper mapper;

    public AdminRatePlanController(
            PricingAdminService adminService,
            PricingAdminQueryService queryService,
            PricingWebMapper mapper) {
        this.adminService = adminService;
        this.queryService = queryService;
        this.mapper = mapper;
    }

    @GetMapping("/rate-plans")
    public List<RatePlanResponse> ratePlans() {
        return queryService.listRatePlans().stream().map(mapper::toResponse).toList();
    }

    @PostMapping("/rate-plans")
    public ResponseEntity<RatePlanResponse> createRatePlan(
            @Valid @RequestBody RatePlanCreateRequest request) {
        RatePlanResponse response =
                mapper.toResponse(
                        adminService.createRatePlan(
                                request.code(),
                                request.nameTr(),
                                request.nameEn(),
                                request.active() == null || request.active()));
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PatchMapping("/rate-plans/{id}")
    public RatePlanResponse updateRatePlan(
            @PathVariable UUID id, @Valid @RequestBody RatePlanUpdateRequest request) {
        return mapper.toResponse(
                adminService.updateRatePlan(
                        id, request.nameTr(), request.nameEn(), request.active()));
    }

    @PutMapping("/rate-plans/{id}/default")
    public RatePlanResponse setDefault(
            @PathVariable UUID id,
            @RequestBody(required = false) SetDefaultRatePlanRequest request) {
        UUID expectedDefaultPlanId =
                request == null ? null : request.expectedDefaultPlanId();
        return mapper.toResponse(adminService.setDefault(id, expectedDefaultPlanId));
    }

    @GetMapping("/rate-plans/{id}/seasons")
    public List<SeasonResponse> seasons(@PathVariable UUID id) {
        return queryService.listSeasons(id).stream().map(mapper::toResponse).toList();
    }

    @PostMapping("/rate-plans/{id}/seasons")
    public ResponseEntity<SeasonResponse> createSeason(
            @PathVariable UUID id, @Valid @RequestBody SeasonCreateRequest request) {
        SeasonResponse response =
                mapper.toResponse(
                        adminService.createSeason(
                                id,
                                request.code(),
                                request.nameTr(),
                                request.nameEn(),
                                request.startDate(),
                                request.endDate()));
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PatchMapping("/seasons/{id}")
    public SeasonResponse updateSeason(
            @PathVariable UUID id, @Valid @RequestBody SeasonUpdateRequest request) {
        return mapper.toResponse(
                adminService.updateSeason(
                        id,
                        request.nameTr(),
                        request.nameEn(),
                        request.startDate(),
                        request.endDate()));
    }

    @DeleteMapping("/seasons/{id}")
    public ResponseEntity<Void> deleteSeason(@PathVariable UUID id) {
        adminService.deleteSeason(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/rate-plans/{id}/prices")
    public PriceMatrixResponse basePrices(@PathVariable UUID id) {
        return mapper.toResponse(queryService.baseMatrix(id));
    }

    @PutMapping("/rate-plans/{id}/prices")
    public PriceMatrixResponse saveBasePrices(
            @PathVariable UUID id, @Valid @RequestBody PriceMatrixRequest request) {
        return mapper.toResponse(adminService.saveBasePrices(id, commands(request)));
    }

    @DeleteMapping("/rate-plans/{id}/prices/{priceRowId}")
    public ResponseEntity<Void> deleteBasePrice(
            @PathVariable UUID id, @PathVariable UUID priceRowId) {
        adminService.deleteBasePrice(id, priceRowId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/seasons/{id}/prices")
    public PriceMatrixResponse seasonPrices(@PathVariable UUID id) {
        return mapper.toResponse(queryService.seasonMatrix(id));
    }

    @PutMapping("/seasons/{id}/prices")
    public PriceMatrixResponse saveSeasonPrices(
            @PathVariable UUID id, @Valid @RequestBody PriceMatrixRequest request) {
        return mapper.toResponse(adminService.saveSeasonPrices(id, commands(request)));
    }

    @DeleteMapping("/seasons/{id}/prices/{priceRowId}")
    public ResponseEntity<Void> deleteSeasonPrice(
            @PathVariable UUID id, @PathVariable UUID priceRowId) {
        adminService.deleteSeasonPrice(id, priceRowId);
        return ResponseEntity.noContent().build();
    }

    private static List<PriceMatrixService.PriceRowCommand> commands(
            PriceMatrixRequest request) {
        return request.rows().stream()
                .map(
                        row ->
                                new PriceMatrixService.PriceRowCommand(
                                        row.roomTypeId(), row.guests(), row.amountTry()))
                .toList();
    }
}
