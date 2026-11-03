package com.hotel.media.internal.application;

import java.time.Duration;

public interface MediaStoragePort {

    PresignedUpload presignPut(String key, String contentType, Duration ttl);

    String presignGet(String key, Duration ttl);

    StoredObject head(String key);

    byte[] get(String key);

    void put(String key, byte[] bytes, String contentType);

    void copy(String sourceKey, String targetKey);

    void delete(String key);

    String publicUrl(String publicKey);

    record PresignedUpload(String url, java.time.Instant expiresAt) {}

    record StoredObject(long sizeBytes, String contentType) {}
}
