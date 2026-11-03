package com.hotel.media.internal.web.dto;

import com.hotel.media.internal.domain.MediaStatus;
import java.util.UUID;

public record CompleteMediaUploadResponse(UUID mediaId, MediaStatus status) {}
