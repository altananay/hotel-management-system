package com.hotel.media.internal.web.dto;

import jakarta.validation.constraints.Size;
import java.util.UUID;

public record UpdateMediaRequest(
        UUID roomTypeId,
        @Size(max = 200) String altTextTr,
        @Size(max = 200) String altTextEn) {}
