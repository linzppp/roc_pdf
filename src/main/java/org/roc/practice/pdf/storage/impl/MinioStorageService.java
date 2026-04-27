package org.roc.practice.pdf.storage.impl;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.roc.practice.pdf.config.MinioConfig;
import org.roc.practice.pdf.storage.StorageService;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;

@Slf4j
@Service
@RequiredArgsConstructor
public class MinioStorageService implements StorageService {

    private final MinioClient minioClient;
    private final MinioConfig minioConfig;

    @PostConstruct
    public void ensureBucketExists() {
        try {
            boolean exists = minioClient.bucketExists(
                    BucketExistsArgs.builder().bucket(minioConfig.getBucket()).build());
            if (!exists) {
                minioClient.makeBucket(
                        MakeBucketArgs.builder().bucket(minioConfig.getBucket()).build());
                log.info("Created MinIO bucket: {}", minioConfig.getBucket());
            }
        } catch (Exception e) {
            throw new IllegalStateException("Failed to initialize MinIO bucket", e);
        }
    }

    @Override
    public void upload(String objectKey, byte[] data) {
        try (ByteArrayInputStream is = new ByteArrayInputStream(data)) {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(minioConfig.getBucket())
                    .object(objectKey)
                    .stream(is, data.length, -1)
                    .contentType("application/pdf")
                    .build());
            log.debug("Uploaded to MinIO: {}/{}", minioConfig.getBucket(), objectKey);
        } catch (Exception e) {
            throw new RuntimeException("MinIO upload failed: " + objectKey, e);
        }
    }

    @Override
    public String buildUrl(String objectKey) {
        // 公开 bucket，URL 格式：{endpoint}/{bucket}/{objectKey}
        String endpoint = minioConfig.getEndpoint();
        String bucket   = minioConfig.getBucket();
        return endpoint.endsWith("/")
                ? endpoint + bucket + "/" + objectKey
                : endpoint + "/" + bucket + "/" + objectKey;
    }
}
