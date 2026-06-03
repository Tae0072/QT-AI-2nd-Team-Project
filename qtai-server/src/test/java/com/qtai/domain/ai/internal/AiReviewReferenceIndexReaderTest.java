package com.qtai.domain.ai.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qtai.common.exception.BusinessException;
import com.qtai.common.exception.ErrorCode;

class AiReviewReferenceIndexReaderTest {

    private static final String INDEX_URI = "restricted://validation/index/reference-index.json";
    private static final String SOURCE_FILE_HASH = "sha256:reference-pdf-hash";

    @TempDir
    Path tempDir;

    private AiReviewReferenceIndexReader reader;
    private Path indexFile;

    @BeforeEach
    void setUp() throws Exception {
        RestrictedStorageUriResolver resolver = new RestrictedStorageUriResolver(tempDir);
        reader = new AiReviewReferenceIndexReader(resolver, new ObjectMapper());
        indexFile = tempDir.resolve("validation").resolve("index").resolve("reference-index.json");
        Files.createDirectories(indexFile.getParent());
    }

    @Test
    void readsValidReferenceIndex() throws Exception {
        Files.writeString(indexFile, validIndexJson(SOURCE_FILE_HASH));

        AiReviewReferenceIndexReader.ReferenceIndex index = reader.read(INDEX_URI, SOURCE_FILE_HASH);

        assertThat(index.schemaVersion()).isEqualTo("ai-review-reference-index.v1");
        assertThat(index.sourceFileHash()).isEqualTo(SOURCE_FILE_HASH);
        assertThat(index.generatedAt()).isNotNull();
        assertThat(index.entries()).hasSize(1);
        AiReviewReferenceIndexReader.ReferenceIndexEntry entry = index.entries().get(0);
        assertThat(entry.bookCode()).isEqualTo("JHN");
        assertThat(entry.chapterStart()).isEqualTo(3);
        assertThat(entry.verseStart()).isEqualTo(16);
        assertThat(entry.chapterEnd()).isEqualTo(3);
        assertThat(entry.verseEnd()).isEqualTo(18);
        assertThat(entry.referenceRangeLabel()).isEqualTo("요한복음 3:16-18");
        assertThat(entry.referenceText()).isEqualTo("해당 범위 검수에 사용할 짧은 해설 참고자료");
        assertThat(entry.referenceHash()).isEqualTo("sha256:entry-reference-hash");
    }

    @Test
    void invalidSchemaVersionIsRejected() throws Exception {
        Files.writeString(indexFile, validIndexJson(SOURCE_FILE_HASH)
                .replace("ai-review-reference-index.v1", "ai-review-reference-index.v0"));

        assertThatThrownBy(() -> reader.read(INDEX_URI, SOURCE_FILE_HASH))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INTERNAL_ERROR))
                .hasMessage("AI_REVIEW_REFERENCE_INDEX_SCHEMA_INVALID");
    }

    @Test
    void sourceFileHashMismatchIsRejected() throws Exception {
        Files.writeString(indexFile, validIndexJson("sha256:other-hash"));

        assertThatThrownBy(() -> reader.read(INDEX_URI, SOURCE_FILE_HASH))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INTERNAL_ERROR))
                .hasMessage("AI_REVIEW_REFERENCE_INDEX_HASH_MISMATCH");
    }

    @Test
    void malformedJsonIsRejected() throws Exception {
        Files.writeString(indexFile, "{ broken json");

        assertThatThrownBy(() -> reader.read(INDEX_URI, SOURCE_FILE_HASH))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INTERNAL_ERROR))
                .hasMessage("AI_REVIEW_REFERENCE_INDEX_INVALID");
    }

    @Test
    void missingRequiredFieldIsRejected() throws Exception {
        Files.writeString(indexFile, validIndexJson(SOURCE_FILE_HASH).replace("\"bookCode\": \"JHN\",", ""));

        assertThatThrownBy(() -> reader.read(INDEX_URI, SOURCE_FILE_HASH))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INTERNAL_ERROR))
                .hasMessage("AI_REVIEW_REFERENCE_INDEX_INVALID");
    }

    @Test
    void emptyEntriesAreRejected() throws Exception {
        Files.writeString(indexFile, """
                {
                  "schemaVersion": "ai-review-reference-index.v1",
                  "sourceFileHash": "sha256:reference-pdf-hash",
                  "generatedAt": "2026-06-04T09:00:00+09:00",
                  "entries": []
                }
                """);

        assertThatThrownBy(() -> reader.read(INDEX_URI, SOURCE_FILE_HASH))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INTERNAL_ERROR))
                .hasMessage("AI_REVIEW_REFERENCE_INDEX_INVALID");
    }

    private static String validIndexJson(String sourceFileHash) {
        return """
                {
                  "schemaVersion": "ai-review-reference-index.v1",
                  "sourceFileHash": "%s",
                  "generatedAt": "2026-06-04T09:00:00+09:00",
                  "entries": [
                    {
                      "bookCode": "JHN",
                      "chapterStart": 3,
                      "verseStart": 16,
                      "chapterEnd": 3,
                      "verseEnd": 18,
                      "referenceRangeLabel": "요한복음 3:16-18",
                      "referenceText": "해당 범위 검수에 사용할 짧은 해설 참고자료",
                      "referenceHash": "sha256:entry-reference-hash"
                    }
                  ]
                }
                """.formatted(sourceFileHash);
    }
}
