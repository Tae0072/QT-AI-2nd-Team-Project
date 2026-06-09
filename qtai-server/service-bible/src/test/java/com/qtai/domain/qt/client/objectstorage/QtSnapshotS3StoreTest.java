package com.qtai.domain.qt.client.objectstorage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

/**
 * {@link QtSnapshotS3Store} 단위 테스트 — mock {@link S3Client}로 putObject 호출과 s3:// 위치 반환을 검증.
 */
class QtSnapshotS3StoreTest {

    @Test
    @DisplayName("지정 버킷/키/JSON으로 putObject를 호출하고 s3:// 위치를 반환한다")
    void puts_object_and_returns_s3_uri() {
        S3Client s3Client = mock(S3Client.class);
        QtSnapshotS3Store store = new QtSnapshotS3Store(s3Client, "qt-bucket");

        String location = store.store("2026-06-09.json", "{\"qtPassageId\":101}");

        assertThat(location).isEqualTo("s3://qt-bucket/2026-06-09.json");

        ArgumentCaptor<PutObjectRequest> requestCaptor = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(s3Client).putObject(requestCaptor.capture(), any(RequestBody.class));
        PutObjectRequest request = requestCaptor.getValue();
        assertThat(request.bucket()).isEqualTo("qt-bucket");
        assertThat(request.key()).isEqualTo("2026-06-09.json");
        assertThat(request.contentType()).isEqualTo("application/json");
    }
}
