package org.roc.practice.pdf.storage.impl;

import io.minio.BucketExistsArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.http.Method;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.roc.practice.pdf.config.MinioConfig;
import org.roc.practice.pdf.storage.StorageService;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class MinioStorageService implements StorageService {

    private final MinioClient minioClient;
    private final MinioConfig minioConfig;

    private final OkHttpClient httpClient = new OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build();

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
        try {
            String presignedUrl = minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.PUT)
                            .bucket(minioConfig.getBucket())
                            .object(objectKey)
                            .expiry(600)
                            .build());

            RequestBody body = RequestBody.create(data, MediaType.parse("application/pdf"));
            Request request = new Request.Builder()
                    .url(presignedUrl)
                    .put(body)
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    throw new RuntimeException("MinIO presigned upload failed: HTTP "
                            + response.code() + " - " + response.message());
                }
            }
            log.debug("Uploaded to MinIO via presigned URL: {}/{}", minioConfig.getBucket(), objectKey);
        } catch (RuntimeException e) {
            throw e;
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
