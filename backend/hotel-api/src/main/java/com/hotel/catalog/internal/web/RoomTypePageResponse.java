package com.hotel.catalog.internal.web;

import com.hotel.catalog.internal.application.RoomCatalogQueryService.PagedRoomTypesResult;
import java.util.List;

public record RoomTypePageResponse(
        List<RoomTypeResponse> items, int page, int size, long total) {

    public static RoomTypePageResponse fromResult(PagedRoomTypesResult result) {
        List<RoomTypeResponse> responses =
                result.items().stream().map(RoomTypeResponse::fromDomain).toList();
        return new RoomTypePageResponse(responses, result.page(), result.size(), result.total());
    }
}
