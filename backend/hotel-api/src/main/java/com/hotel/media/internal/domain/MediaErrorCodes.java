package com.hotel.media.internal.domain;

public final class MediaErrorCodes {

    public static final String NOT_FOUND = "media.not_found";
    public static final String UPLOAD_EXPIRED = "media.upload_expired";
    public static final String INVALID_CONTENT_TYPE = "media.invalid_content_type";
    public static final String FILE_TOO_LARGE = "media.file_too_large";
    public static final String IMAGE_TOO_LARGE = "media.image_too_large";
    public static final String INVALID_IMAGE = "media.invalid_image";
    public static final String ROOM_TYPE_NOT_FOUND = "media.room_type_not_found";
    public static final String NOT_READY = "media.not_ready";
    public static final String NOT_PUBLISHED = "media.not_published";
    public static final String COVER_CONFLICT = "media.cover_conflict";
    public static final String ORDER_CONFLICT = "media.order_conflict";
    public static final String ALREADY_ARCHIVED = "media.already_archived";
    public static final String PROCESSING_FAILED = "media.processing_failed";
    public static final String ALT_TEXT_INVALID = "media.alt_text_invalid";
    public static final String STORAGE_UNAVAILABLE = "media.storage_unavailable";
    public static final String SERVICE_UNAVAILABLE = "media.service_unavailable";

    private MediaErrorCodes() {}
}
