package com.hotel.media.internal.web.dto;

import java.util.List;

public record MediaPageResponse(
        List<MediaAdminItemResponse> items, int page, int size, long total) {}
