package com.hotel.media.internal.web.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

public record ReorderMediaRequest(
        @NotNull UUID roomTypeId, @NotEmpty List<UUID> mediaIds) {}
