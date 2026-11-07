package com.hotel.media.internal.web.dto;

public record PublicMediaVariantsResponse(
        PublicMediaVariantResponse thumb,
        PublicMediaVariantResponse card,
        PublicMediaVariantResponse cover) {}
