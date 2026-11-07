package com.hotel.media.internal.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record CreateMediaUploadRequest(
        @NotBlank String fileName, @NotBlank String contentType, @Positive long sizeBytes) {}
