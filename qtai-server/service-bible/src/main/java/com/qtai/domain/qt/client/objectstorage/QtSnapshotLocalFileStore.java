package com.qtai.domain.qt.client.objectstorage;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import com.qtai.common.exception.BusinessException;
import com.qtai.common.exception.ErrorCode;
import com.qtai.domain.qt.internal.QtSnapshotStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * QT 스냅샷 로컬 파일 어댑터 — 기본 구현(외부 의존성 없음).
 *
 * <p>{@code qt.snapshot.storage}가 없거나 {@code local}이면 활성화된다. 로컬 개발·테스트·기동 검증에서
 * S3 계정 없이 동작하며, 배포 시에는 {@code qt.snapshot.storage=s3}로 S3 어댑터로 교체된다.
 */
@Component
@ConditionalOnProperty(name = "qt.snapshot.storage", havingValue = "local", matchIfMissing = true)
public class QtSnapshotLocalFileStore implements QtSnapshotStore {

    private final Path baseDir;

    public QtSnapshotLocalFileStore(@Value("${qt.snapshot.local.dir:./qt-snapshots}") String baseDir) {
        this.baseDir = Path.of(baseDir);
    }

    @Override
    public String store(String objectKey, String json) {
        try {
            Files.createDirectories(baseDir);
            Path target = baseDir.resolve(objectKey);
            Files.writeString(target, json, StandardCharsets.UTF_8);
            return target.toAbsolutePath().toString();
        } catch (IOException exception) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "QT snapshot local write failed: " + objectKey);
        }
    }
}
