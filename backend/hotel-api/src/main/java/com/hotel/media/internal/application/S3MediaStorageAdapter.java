package com.hotel.media.internal.application;

import com.hotel.media.api.MediaStorageException;
import java.time.Duration;
import java.time.Instant;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CopyObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

public class S3MediaStorageAdapter implements MediaStoragePort {

    private final S3Client client;
    private final S3Presigner presigner;
    private final String bucket;
    private final String publicBaseUrl;
    private final Duration defaultTtl;

    public S3MediaStorageAdapter(
            S3Client client,
            S3Presigner presigner,
            String bucket,
            String publicBaseUrl,
            Duration defaultTtl) {
        this.client = client;
        this.presigner = presigner;
        this.bucket = bucket;
        this.publicBaseUrl = publicBaseUrl.replaceAll("/$", "");
        this.defaultTtl = defaultTtl;
    }

    @Override
    public PresignedUpload presignPut(String key, String contentType, Duration ttl) {
        Duration effectiveTtl = ttl == null ? defaultTtl : ttl;
        PutObjectRequest request =
                PutObjectRequest.builder()
                        .bucket(bucket)
                        .key(key)
                        .contentType(contentType)
                        .build();
        PresignedPutObjectRequest presigned =
                presigner.presignPutObject(
                        PutObjectPresignRequest.builder()
                                .signatureDuration(effectiveTtl)
                                .putObjectRequest(request)
                                .build());
        return new PresignedUpload(
                presigned.url().toString(), Instant.now().plus(effectiveTtl));
    }

    @Override
    public String presignGet(String key, Duration ttl) {
        Duration effectiveTtl = ttl == null ? defaultTtl : ttl;
        PresignedGetObjectRequest presigned =
                presigner.presignGetObject(
                        GetObjectPresignRequest.builder()
                                .signatureDuration(effectiveTtl)
                                .getObjectRequest(
                                        GetObjectRequest.builder()
                                                .bucket(bucket)
                                                .key(key)
                                                .build())
                                .build());
        return presigned.url().toString();
    }

    @Override
    public StoredObject head(String key) {
        try {
            var response =
                    client.headObject(
                            HeadObjectRequest.builder().bucket(bucket).key(key).build());
            return new StoredObject(response.contentLength(), response.contentType());
        } catch (software.amazon.awssdk.services.s3.model.NoSuchKeyException ex) {
            return null;
        } catch (software.amazon.awssdk.services.s3.model.S3Exception ex) {
            if (ex.statusCode() == 404) {
                return null;
            }
            throw new MediaStorageException("Could not inspect stored object.", ex);
        } catch (RuntimeException ex) {
            throw new MediaStorageException("Could not inspect stored object.", ex);
        }
    }

    @Override
    public byte[] get(String key) {
        try {
            return client.getObjectAsBytes(
                            GetObjectRequest.builder().bucket(bucket).key(key).build())
                    .asByteArray();
        } catch (RuntimeException ex) {
            throw new MediaStorageException("Could not read stored object.", ex);
        }
    }

    @Override
    public void put(String key, byte[] bytes, String contentType) {
        try {
            client.putObject(
                    PutObjectRequest.builder()
                            .bucket(bucket)
                            .key(key)
                            .contentType(contentType)
                            .build(),
                    RequestBody.fromBytes(bytes));
        } catch (RuntimeException ex) {
            throw new MediaStorageException("Could not write stored object.", ex);
        }
    }

    @Override
    public void copy(String sourceKey, String targetKey) {
        try {
            client.copyObject(
                    CopyObjectRequest.builder()
                            .sourceBucket(bucket)
                            .sourceKey(sourceKey)
                            .destinationBucket(bucket)
                            .destinationKey(targetKey)
                            .contentType(contentTypeFor(targetKey))
                            .metadataDirective("REPLACE")
                            .cacheControl("public, max-age=31536000, immutable")
                            .build());
        } catch (RuntimeException ex) {
            throw new MediaStorageException("Could not publish stored object.", ex);
        }
    }

    private static String contentTypeFor(String key) {
        return key.endsWith(".webp") ? "image/webp" : "image/jpeg";
    }

    @Override
    public void delete(String key) {
        try {
            client.deleteObject(
                    DeleteObjectRequest.builder().bucket(bucket).key(key).build());
        } catch (RuntimeException ex) {
            throw new MediaStorageException("Could not delete stored object.", ex);
        }
    }

    @Override
    public String publicUrl(String publicKey) {
        String relative =
                publicKey.startsWith("public/")
                        ? publicKey.substring("public/".length())
                        : publicKey;
        return publicBaseUrl + "/" + relative;
    }
}
