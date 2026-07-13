package com.edunexus.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.time.Duration;

@Service
@Slf4j
public class S3Service {

    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    @Value("${aws.s3.presigned-url-duration:3600}")
    private int presignedUrlDuration;

    private S3Client s3Client = S3Client.builder().build();
    private S3Presigner presigner = S3Presigner.builder().build();

    public String generatePresignedUrl(String objectKey) {
        try {
            GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                    .signatureDuration(Duration.ofSeconds(presignedUrlDuration))
                    .getObjectRequest(r -> r.bucket(bucketName).key(objectKey))
                    .build();

            PresignedGetObjectRequest presignedRequest = presigner.presignGetObject(presignRequest);
            String presignedUrl = presignedRequest.url().toString();

            log.debug("Generated presigned URL for object: {}", objectKey);
            return presignedUrl;
        } catch (Exception e) {
            log.error("Error generating presigned URL for object: {}", objectKey, e);
            throw new RuntimeException("Failed to generate presigned URL", e);
        }
    }

    public boolean objectExists(String objectKey) {
        try {
            HeadObjectRequest headRequest = HeadObjectRequest.builder()
                    .bucket(bucketName)
                    .key(objectKey)
                    .build();
            s3Client.headObject(headRequest);
            return true;
        } catch (Exception e) {
            log.debug("Object does not exist: {}", objectKey);
            return false;
        }
    }

    public void closeResources() {
        try {
            if (s3Client != null) {
                s3Client.close();
            }
            if (presigner != null) {
                presigner.close();
            }
        } catch (Exception e) {
            log.error("Error closing S3 resources", e);
        }
    }
}
