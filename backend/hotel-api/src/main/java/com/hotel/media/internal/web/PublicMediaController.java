package com.hotel.media.internal.web;

import com.hotel.media.internal.application.MediaPublicQueryService;
import com.hotel.media.internal.web.dto.PublicMediaItemResponse;
import com.hotel.media.internal.web.dto.PublicMediaPageResponse;
import com.hotel.sharedkernel.api.DomainError;
import com.hotel.sharedkernel.api.DomainErrorCategory;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PublicMediaController {

    private final MediaPublicQueryService publicQueryService;

    public PublicMediaController(MediaPublicQueryService publicQueryService) {
        this.publicQueryService = publicQueryService;
    }

    @GetMapping("/api/media")
    public ResponseEntity<PublicMediaPageResponse> list(
            @RequestParam(name = "roomTypeIds") String roomTypeIds) {
        List<UUID> ids;
        try {
            ids =
                    Arrays.stream(roomTypeIds.split(","))
                            .map(String::trim)
                            .filter(value -> !value.isEmpty())
                            .map(UUID::fromString)
                            .distinct()
                            .toList();
        } catch (IllegalArgumentException ex) {
            throw new DomainError(
                    "validation.invalid_request",
                    "roomTypeIds contains an invalid UUID.",
                    DomainErrorCategory.VALIDATION);
        }
        List<PublicMediaItemResponse> items =
                publicQueryService.findPublished(ids).stream()
                        .map(PublicMediaItemResponse::from)
                        .toList();
        // Publish/unpublish must be visible immediately; image objects keep their immutable cache.
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(new PublicMediaPageResponse(items));
    }
}
