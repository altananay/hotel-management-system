package com.hotel.pricing.internal.web;

import com.hotel.pricing.internal.application.PricingAdminQueryService;
import com.hotel.pricing.internal.application.PricingAdminService;
import com.hotel.pricing.internal.web.dto.FxRateHistoryResponse;
import com.hotel.pricing.internal.web.dto.RoomTypeOptionResponse;
import com.hotel.pricing.internal.web.dto.SettingsResponse;
import com.hotel.pricing.internal.web.dto.SettingsUpdateRequest;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin-api/pricing")
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'MANAGER')")
public class AdminPricingSettingsController {

    private final PricingAdminService adminService;
    private final PricingAdminQueryService queryService;

    public AdminPricingSettingsController(
            PricingAdminService adminService, PricingAdminQueryService queryService) {
        this.adminService = adminService;
        this.queryService = queryService;
    }

    @GetMapping("/settings")
    public SettingsResponse settings() {
        return queryService.settings();
    }

    @PutMapping("/settings")
    public SettingsResponse updateSettings(@Valid @RequestBody SettingsUpdateRequest request) {
        return adminService.updateSettings(request);
    }

    @GetMapping("/fx-rates")
    public FxRateHistoryResponse fxRates(
            @RequestParam(required = false) String currency,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return queryService.fxRates(currency, from, to, page, size);
    }

    @GetMapping("/room-types")
    public List<RoomTypeOptionResponse> roomTypes() {
        return queryService.activeRoomTypes();
    }
}
