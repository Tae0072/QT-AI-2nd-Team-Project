package com.qtai.domain.ai.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.OffsetDateTime;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import com.qtai.common.exception.BusinessException;
import com.qtai.common.exception.ErrorCode;

class AiReviewReferenceExcerptSelectorTest {

    private static final OffsetDateTime GENERATED_AT = OffsetDateTime.parse("2026-06-04T09:00:00+09:00");

    private final AiReviewReferenceExcerptSelector selector =
            new AiReviewReferenceExcerptSelector(new ObjectMapper());

    @Test
    void selectsEntryWhenAssetVerseIsInsideReferenceRange() {
        List<AiReviewReferenceExcerptSelector.SelectedExcerpt> selected =
                selector.select(assetPayload("JHN", 3, 16), index(entry("JHN", 3, 16, 3, 18, "hash-1", "본문 검수 참고")));

        assertThat(selected).hasSize(1);
        AiReviewReferenceExcerptSelector.SelectedExcerpt excerpt = selected.get(0);
        assertThat(excerpt.bookCode()).isEqualTo("JHN");
        assertThat(excerpt.chapterStart()).isEqualTo(3);
        assertThat(excerpt.verseStart()).isEqualTo(16);
        assertThat(excerpt.chapterEnd()).isEqualTo(3);
        assertThat(excerpt.verseEnd()).isEqualTo(18);
        assertThat(excerpt.referenceRangeLabel()).isEqualTo("JHN 3:16-18");
        assertThat(excerpt.referenceHash()).isEqualTo("hash-1");
        assertThat(excerpt.referenceText()).isEqualTo("본문 검수 참고");
    }

    @Test
    void doesNotSelectEntryWhenBookCodeIsDifferent() {
        List<AiReviewReferenceExcerptSelector.SelectedExcerpt> selected =
                selector.select(assetPayload("JHN", 3, 16), index(entry("ROM", 3, 16, 3, 18, "hash-1", "본문 검수 참고")));

        assertThat(selected).isEmpty();
    }

    @Test
    void keepsIndexOrderAndLimitsToThreeEntries() {
        List<AiReviewReferenceExcerptSelector.SelectedExcerpt> selected = selector.select(
                assetPayload("JHN", 3, 16),
                index(
                        entry("JHN", 3, 16, 3, 16, "hash-1", "첫 번째"),
                        entry("JHN", 3, 16, 3, 16, "hash-2", "두 번째"),
                        entry("JHN", 3, 16, 3, 16, "hash-3", "세 번째"),
                        entry("JHN", 3, 16, 3, 16, "hash-4", "네 번째")
                )
        );

        assertThat(selected).extracting(AiReviewReferenceExcerptSelector.SelectedExcerpt::referenceHash)
                .containsExactly("hash-1", "hash-2", "hash-3");
    }

    @Test
    void truncatesReferenceTextToPromptLimit() {
        String longText = "가".repeat(1_205);

        List<AiReviewReferenceExcerptSelector.SelectedExcerpt> selected =
                selector.select(assetPayload("JHN", 3, 16), index(entry("JHN", 3, 16, 3, 16, "hash-1", longText)));

        assertThat(selected).hasSize(1);
        assertThat(selected.get(0).referenceText()).hasSize(1_200);
        assertThat(selected.get(0).referenceText()).isEqualTo("가".repeat(1_200));
    }

    @Test
    void missingVerseMetadataIsRejected() {
        String payloadJson = """
                {
                  "explanations": [
                    {"verseId": 1001, "summary": "summary", "explanation": "explanation"}
                  ],
                  "sourceMetadata": {"verseIds": [1001]}
                }
                """;

        assertThatThrownBy(() -> selector.select(payloadJson, index(entry("JHN", 3, 16, 3, 16, "hash-1", "본문 검수 참고"))))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT))
                .hasMessage("AI_REVIEW_ASSET_VERSE_METADATA_NOT_FOUND");
    }

    private static String assetPayload(String bookCode, int chapterNo, int verseNo) {
        return """
                {
                  "explanations": [
                    {"verseId": 1001, "summary": "summary", "explanation": "explanation"}
                  ],
                  "sourceMetadata": {
                    "verseIds": [1001],
                    "verses": [
                      {"verseId": 1001, "bookCode": "%s", "chapterNo": %d, "verseNo": %d}
                    ]
                  }
                }
                """.formatted(bookCode, chapterNo, verseNo);
    }

    private static AiReviewReferenceIndexReader.ReferenceIndex index(
            AiReviewReferenceIndexReader.ReferenceIndexEntry... entries
    ) {
        return new AiReviewReferenceIndexReader.ReferenceIndex(
                "ai-review-reference-index.v1",
                "sha256:reference-hash",
                GENERATED_AT,
                List.of(entries)
        );
    }

    private static AiReviewReferenceIndexReader.ReferenceIndexEntry entry(
            String bookCode,
            int chapterStart,
            int verseStart,
            int chapterEnd,
            int verseEnd,
            String referenceHash,
            String referenceText
    ) {
        return new AiReviewReferenceIndexReader.ReferenceIndexEntry(
                bookCode,
                chapterStart,
                verseStart,
                chapterEnd,
                verseEnd,
                bookCode + " " + chapterStart + ":" + verseStart + "-" + verseEnd,
                referenceText,
                referenceHash
        );
    }
}
