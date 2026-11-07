package com.hotel.media.internal.application;

import java.net.URI;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@Configuration
public class MediaS3Config {

    @Bean
    S3Client mediaS3Client(
            @Value("${hotel.media.s3.endpoint:http://localhost:9000}") String endpoint,
            @Value("${hotel.media.s3.region:us-east-1}") String region,
            @Value("${hotel.media.s3.access-key:minioadmin}") String accessKey,
            @Value("${hotel.media.s3.secret-key:minioadmin_local_dev}") String secretKey) {
        return S3Client.builder()
                .endpointOverride(URI.create(endpoint))
                .region(Region.of(region))
                .credentialsProvider(
                        StaticCredentialsProvider.create(
                                AwsBasicCredentials.create(accessKey, secretKey)))
                .serviceConfiguration(
                        S3Configuration.builder().pathStyleAccessEnabled(true).build())
                .build();
    }

    @Bean
    S3Presigner mediaS3Presigner(
            @Value("${hotel.media.s3.public-endpoint:http://localhost:9000}") String publicEndpoint,
            @Value("${hotel.media.s3.region:us-east-1}") String region,
            @Value("${hotel.media.s3.access-key:minioadmin}") String accessKey,
            @Value("${hotel.media.s3.secret-key:minioadmin_local_dev}") String secretKey) {
        return S3Presigner.builder()
                .endpointOverride(URI.create(publicEndpoint))
                .region(Region.of(region))
                .credentialsProvider(
                        StaticCredentialsProvider.create(
                                AwsBasicCredentials.create(accessKey, secretKey)))
                .serviceConfiguration(
                        S3Configuration.builder().pathStyleAccessEnabled(true).build())
                .build();
    }

    @Bean
    MediaStoragePort mediaStoragePort(
            S3Client mediaS3Client,
            S3Presigner mediaS3Presigner,
            @Value("${hotel.media.s3.bucket:hotel-media}") String bucket,
            @Value("${hotel.media.public-base-url:http://localhost:9000/hotel-media/public}")
                    String publicBaseUrl,
            @Value("${hotel.media.upload.presign-ttl:10m}") Duration ttl) {
        return new S3MediaStorageAdapter(
                mediaS3Client, mediaS3Presigner, bucket, publicBaseUrl, ttl);
    }
}
