package com.hotel.media.internal.web.dto;

import java.time.Instant;
import java.util.UUID;

public record CreateMediaUploadResponse(UUID mediaId, String uploadUrl, Instant expiresAt) {}
