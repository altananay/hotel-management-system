package com.hotel.media.internal.web.dto;

import com.hotel.media.internal.application.MediaPublicQueryService.PublicMediaView;
import com.hotel.media.internal.application.MediaPublicQueryService.PublicVariant;
import com.hotel.media.internal.domain.MediaVariantType;
import java.util.Map;
import java.util.UUID;

public record PublicMediaItemResponse(
        UUID id,
        UUID roomTypeId,
        boolean cover,
        int displayOrder,
        String altTextTr,
        String altTextEn,
        PublicMediaVariantsResponse variants) {

    public static PublicMediaItemResponse from(PublicMediaView view) {
        Map<MediaVariantType, PublicVariant> variants = view.variants();
        return new PublicMediaItemResponse(
                view.id(),
                view.roomTypeId(),
                view.cover(),
                view.displayOrder(),
                view.altTextTr(),
                view.altTextEn(),
                new PublicMediaVariantsResponse(
                        variant(variants.get(MediaVariantType.THUMB)),
                        variant(variants.get(MediaVariantType.CARD)),
                        variant(variants.get(MediaVariantType.COVER))));
    }

    private static PublicMediaVariantResponse variant(PublicVariant variant) {
        return variant == null
                ? null
                : new PublicMediaVariantResponse(
                        variant.url(), variant.width(), variant.height(), variant.format());
    }
}
