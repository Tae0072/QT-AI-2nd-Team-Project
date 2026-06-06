package com.qtai.domain.ai.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

class AiReviewReferenceBookSectionMapReaderTest {

    private static final String SOURCE_FILE_HASH = "sha256:source-hash";

    private final AiReviewReferenceBookSectionMapReader reader =
            new AiReviewReferenceBookSectionMapReader(new ObjectMapper());

    @Test
    void readsValidBookSectionMap() {
        AiReviewReferenceBookSectionMapReader.BookSectionMap map =
                reader.parse(validMapJson(SOURCE_FILE_HASH), SOURCE_FILE_HASH);

        assertThat(map.sourceFileName()).isEqualTo("reference.pdf");
        assertThat(map.sourceFileHash()).isEqualTo(SOURCE_FILE_HASH);
        assertThat(map.sections()).hasSize(2);
        assertThat(map.sectionForPage(15).bookCode()).isEqualTo("MAT");
        assertThat(map.sectionForPage(35).bookCode()).isEqualTo("MRK");
        assertThat(map.sectionForPage(99)).isNull();
    }

    @Test
    void invalidSchemaVersionIsRejected() {
        assertThatThrownBy(() -> reader.parse(
                validMapJson(SOURCE_FILE_HASH)
                        .replace("ai-review-reference-book-section-map.v1", "v0"),
                SOURCE_FILE_HASH
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("AI_REVIEW_REFERENCE_BOOK_SECTION_MAP_SCHEMA_INVALID");
    }

    @Test
    void sourceFileHashMismatchIsRejected() {
        assertThatThrownBy(() -> reader.parse(validMapJson("sha256:other"), SOURCE_FILE_HASH))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("AI_REVIEW_REFERENCE_BOOK_SECTION_MAP_HASH_MISMATCH");
    }

    @Test
    void overlappingPageRangeIsRejected() {
        String json = """
                {
                  "schemaVersion": "ai-review-reference-book-section-map.v1",
                  "sourceFileName": "reference.pdf",
                  "sourceFileHash": "sha256:source-hash",
                  "sections": [
                    {"bookCode": "MAT", "bookName": "마태복음", "pageStart": 1, "pageEnd": 20},
                    {"bookCode": "MRK", "bookName": "마가복음", "pageStart": 20, "pageEnd": 30}
                  ]
                }
                """;

        assertThatThrownBy(() -> reader.parse(json, SOURCE_FILE_HASH))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("AI_REVIEW_REFERENCE_BOOK_SECTION_MAP_RANGE_OVERLAPPED");
    }

    @Test
    void reversedPageRangeIsRejected() {
        String json = validMapJson(SOURCE_FILE_HASH)
                .replace("\"pageStart\": 1, \"pageEnd\": 20", "\"pageStart\": 20, \"pageEnd\": 1");

        assertThatThrownBy(() -> reader.parse(json, SOURCE_FILE_HASH))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("AI_REVIEW_REFERENCE_BOOK_SECTION_MAP_RANGE_INVALID");
    }

    @Test
    void missingRequiredFieldIsRejected() {
        assertThatThrownBy(() -> reader.parse(
                validMapJson(SOURCE_FILE_HASH).replace("\"bookName\": \"마태복음\", ", ""),
                SOURCE_FILE_HASH
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("AI_REVIEW_REFERENCE_BOOK_SECTION_MAP_INVALID");
    }

    static String validMapJson(String sourceFileHash) {
        return """
                {
                  "schemaVersion": "ai-review-reference-book-section-map.v1",
                  "sourceFileName": "reference.pdf",
                  "sourceFileHash": "%s",
                  "sections": [
                    {"bookCode": "MAT", "bookName": "마태복음", "pageStart": 1, "pageEnd": 20},
                    {"bookCode": "MRK", "bookName": "마가복음", "pageStart": 21, "pageEnd": 40}
                  ]
                }
                """.formatted(sourceFileHash);
    }
}
