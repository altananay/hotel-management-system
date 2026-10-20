package com.hotel.catalog.internal.web;

import com.hotel.catalog.internal.application.RoomCatalogQueryService;
import com.hotel.catalog.internal.application.RoomCatalogQueryService.PagedRoomTypesResult;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/room-types")
public class RoomTypeController {

    private final RoomCatalogQueryService roomCatalogQueryService;

    public RoomTypeController(RoomCatalogQueryService roomCatalogQueryService) {
        this.roomCatalogQueryService = roomCatalogQueryService;
    }

    @GetMapping
    public RoomTypePageResponse listRoomTypes(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        PagedRoomTypesResult result = roomCatalogQueryService.listActiveRoomTypes(page, size);
        return RoomTypePageResponse.fromResult(result);
    }
}
