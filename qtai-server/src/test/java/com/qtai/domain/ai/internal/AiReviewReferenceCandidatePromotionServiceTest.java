package com.qtai.domain.ai.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class AiReviewReferenceCandidatePromotionServiceTest {

    private static final String SOURCE_FILE_HASH = "sha256:source-hash";

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final AiReviewReferenceBookSectionMapReader mapReader =
            new AiReviewReferenceBookSectionMapReader(objectMapper);
    private final AiReviewReferenceCandidatePromotionService service =
            new AiReviewReferenceCandidatePromotionService(objectMapper);

    @Test
    void promotesCandidateEntryWithBookCodeFromPageSection() {
        AiReviewReferenceCandidatePromotionService.PromotionResult result =
                service.promote(candidateJson(), sectionMap());

        assertThat(result.promotedIndex().schemaVersion()).isEqualTo("ai-review-reference-index.v1");
        assertThat(result.promotedIndex().sourceFileHash()).isEqualTo(SOURCE_FILE_HASH);
        assertThat(result.promotedIndex().entries()).hasSize(1);
        AiReviewReferenceCandidatePromotionService.PromotedEntry entry =
                result.promotedIndex().entries().get(0);
        assertThat(entry.bookCode()).isEqualTo("MAT");
        assertThat(entry.referenceRangeLabel()).isEqualTo("마태복음 19:8");
        assertThat(entry.referenceText()).isEqualTo("승격 대상 참고 본문");
        assertThat(entry.referenceHash()).isEqualTo("sha256:entry-1");
        assertThat(result.summary().candidateEntryCount()).isEqualTo(3);
        assertThat(result.summary().promotedEntryCount()).isEqualTo(1);
    }

    @Test
    void excludesUnusableAndUnmappedEntries() {
        AiReviewReferenceCandidatePromotionService.PromotionResult result =
                service.promote(candidateJson(), sectionMap());

        assertThat(result.summary().unusableEntryCount()).isEqualTo(1);
        assertThat(result.summary().unmappedEntryCount()).isEqualTo(1);
        assertThat(result.promotedIndex().entries()).extracting(
                AiReviewReferenceCandidatePromotionService.PromotedEntry::referenceHash
        ).containsExactly("sha256:entry-1");
    }

    @Test
    void summaryJsonDoesNotContainReferenceText() {
        AiReviewReferenceCandidatePromotionService.PromotionResult result =
                service.promote(candidateJson(), sectionMap());

        String summaryJson = service.summaryJson(result);

        assertThat(summaryJson).doesNotContain("referenceText");
        assertThat(summaryJson).doesNotContain("승격 대상 참고 본문");
    }

    @Test
    void invalidCandidateSchemaIsRejected() {
        assertThatThrownBy(() -> service.promote(
                candidateJson().replace("ai-review-reference-index-candidate.v1", "v0"),
                sectionMap()
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("AI_REVIEW_REFERENCE_CANDIDATE_INDEX_SCHEMA_INVALID");
    }

    @Test
    void candidateHashMismatchIsRejected() {
        assertThatThrownBy(() -> service.promote(
                candidateJson().replace(SOURCE_FILE_HASH, "sha256:other"),
                sectionMap()
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("AI_REVIEW_REFERENCE_CANDIDATE_INDEX_HASH_MISMATCH");
    }

    private AiReviewReferenceBookSectionMapReader.BookSectionMap sectionMap() {
        return mapReader.parse(AiReviewReferenceBookSectionMapReaderTest.validMapJson(SOURCE_FILE_HASH), SOURCE_FILE_HASH);
    }

    private static String candidateJson() {
        return """
                {
                  "schemaVersion": "ai-review-reference-index-candidate.v1",
                  "sourceFileName": "reference.pdf",
                  "sourceFileHash": "sha256:source-hash",
                  "generatedAt": "2026-06-04T10:00:00+09:00",
                  "qualitySummary": {
                    "totalEntryCount": 3,
                    "usableEntryCount": 1,
                    "needsReviewEntryCount": 1,
                    "unusableEntryCount": 1,
                    "averageHangulRatio": 0.8,
                    "averageSuspiciousMojibakeRatio": 0.0,
                    "totalReplacementCharCount": 0
                  },
                  "entries": [
                    {
                      "pageStart": 10,
                      "detectedHeading": "19:8",
                      "bookCode": null,
                      "chapterStart": 19,
                      "verseStart": 8,
                      "chapterEnd": 19,
                      "verseEnd": 8,
                      "referenceRangeLabel": "19:8",
                      "referenceText": "승격 대상 참고 본문",
                      "referenceHash": "sha256:entry-1",
                      "quality": {"status": "NEEDS_REVIEW", "reasons": ["BOOK_CODE_NOT_DETECTED"]}
                    },
                    {
                      "pageStart": 15,
                      "detectedHeading": "19:9",
                      "bookCode": null,
                      "chapterStart": 19,
                      "verseStart": 9,
                      "chapterEnd": 19,
                      "verseEnd": 9,
                      "referenceRangeLabel": "19:9",
                      "referenceText": "제외 대상 참고 본문",
                      "referenceHash": "sha256:entry-2",
                      "quality": {"status": "UNUSABLE", "reasons": ["TEXT_TOO_SHORT"]}
                    },
                    {
                      "pageStart": 99,
                      "detectedHeading": "1:1",
                      "bookCode": null,
                      "chapterStart": 1,
                      "verseStart": 1,
                      "chapterEnd": 1,
                      "verseEnd": 1,
                      "referenceRangeLabel": "1:1",
                      "referenceText": "매핑 실패 참고 본문",
                      "referenceHash": "sha256:entry-3",
                      "quality": {"status": "NEEDS_REVIEW", "reasons": ["BOOK_CODE_NOT_DETECTED"]}
                    }
                  ]
                }
                """;
    }
}
