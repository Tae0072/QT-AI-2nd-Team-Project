package com.qtai.domain.qt.client.objectstorage;

import java.net.URI;
import java.nio.charset.StandardCharsets;

import com.qtai.common.exception.BusinessException;
import com.qtai.common.exception.ErrorCode;
import com.qtai.domain.qt.internal.QtSnapshotStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

/**
 * QT 스냅샷 S3 호환 오브젝트 스토리지 어댑터 (배포: S3/R2, 로컬 시연: MinIO).
 *
 * <p>{@code qt.snapshot.storage=s3}일 때만 활성화된다. endpoint override + path-style 접근으로
 * MinIO·R2 모두 동일 코드로 붙는다. 자격증명은 env로만 주입하며 로그에 남기지 않는다(CLAUDE.md §7).
 */
@Component
@ConditionalOnProperty(name = "qt.snapshot.storage", havingValue = "s3")
public class QtSnapshotS3Store implements QtSnapshotStore {

    private final S3Client s3Client;
    private final String bucket;

    public QtSnapshotS3Store(
            @Value("${qt.snapshot.s3.bucket}") String bucket,
            @Value("${qt.snapshot.s3.endpoint:}") String endpoint,
            @Value("${qt.snapshot.s3.region:us-east-1}") String region,
            @Value("${qt.snapshot.s3.access-key}") String accessKey,
            @Value("${qt.snapshot.s3.secret-key}") String secretKey
    ) {
        this.bucket = bucket;
        var builder = S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKey, secretKey)))
                // MinIO·R2 호환을 위해 path-style 사용.
                .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build());
        if (endpoint != null && !endpoint.isBlank()) {
            builder = builder.endpointOverride(URI.create(endpoint));
        }
        this.s3Client = builder.build();
    }

    /** 테스트용 — 외부 네트워크 없이 mock {@link S3Client}로 동작 검증. */
    QtSnapshotS3Store(S3Client s3Client, String bucket) {
        this.s3Client = s3Client;
        this.bucket = bucket;
    }

    @Override
    public String store(String objectKey, String json) {
        try {
            s3Client.putObject(
                    PutObjectRequest.builder()
                            .bucket(bucket)
                            .key(objectKey)
                            .contentType("application/json")
                            .build(),
                    RequestBody.fromString(json, StandardCharsets.UTF_8));
            return "s3://" + bucket + "/" + objectKey;
        } catch (S3Exception exception) {
            // 키만 남기고 자격증명·본문은 로깅하지 않는다.
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "QT snapshot S3 upload failed: " + objectKey);
        }
    }
}
