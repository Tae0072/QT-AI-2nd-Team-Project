package com.qtai.domain.qt.client.objectstorage;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * {@link QtSnapshotLocalFileStore} 단위 테스트 — 기본(로컬 파일) 어댑터가 키 이름으로 JSON을 기록한다.
 */
class QtSnapshotLocalFileStoreTest {

    @Test
    @DisplayName("baseDir 아래 objectKey 파일로 JSON을 기록하고 절대경로를 반환한다")
    void writes_json_file_under_base_dir(@TempDir Path tempDir) throws Exception {
        QtSnapshotLocalFileStore store = new QtSnapshotLocalFileStore(tempDir.toString());
        String json = "{\"qtPassageId\":101,\"date\":\"2026-06-09\"}";

        String location = store.store("2026-06-09.json", json);

        Path written = tempDir.resolve("2026-06-09.json");
        assertThat(Files.exists(written)).isTrue();
        assertThat(Files.readString(written, StandardCharsets.UTF_8)).isEqualTo(json);
        assertThat(location).isEqualTo(written.toAbsolutePath().toString());
    }
}
