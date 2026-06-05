package com.qtai.domain.ai.internal;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;

class AiReviewReferenceBookSectionMapCandidateGeneratorTest {

    private final AiReviewReferenceBookSectionMapCandidateGenerator generator =
            new AiReviewReferenceBookSectionMapCandidateGenerator(
                    new AiReviewReferenceBookSectionTitleDetector()
            );

    @Test
    void calculatesPageRangesFromDetectedBookStartPages() {
        AiReviewReferenceBookSectionMapCandidateWriter.CandidateDocument document =
                generator.documentFromPages(
                        "reference.pdf",
                        "sha256:source-hash",
                        OffsetDateTime.parse("2026-06-04T10:00:00+09:00"),
                        List.of(
                                new AiReviewReferenceBookSectionMapCandidateGenerator.PageText(3, "창세기"),
                                new AiReviewReferenceBookSectionMapCandidateGenerator.PageText(10, "출애굽기"),
                                new AiReviewReferenceBookSectionMapCandidateGenerator.PageText(12, "본문")
                        )
                );

        assertThat(document.sections()).hasSize(2);
        assertThat(document.sections().get(0).bookCode()).isEqualTo("GEN");
        assertThat(document.sections().get(0).pageStart()).isEqualTo(3);
        assertThat(document.sections().get(0).pageEnd()).isEqualTo(9);
        assertThat(document.sections().get(1).bookCode()).isEqualTo("EXO");
        assertThat(document.sections().get(1).pageStart()).isEqualTo(10);
        assertThat(document.sections().get(1).pageEnd()).isEqualTo(12);
    }

    @Test
    void recordsDuplicateDetectionsAndMissingBooksInSummary() {
        AiReviewReferenceBookSectionMapCandidateWriter.CandidateDocument document =
                generator.documentFromPages(
                        "reference.pdf",
                        "sha256:source-hash",
                        OffsetDateTime.parse("2026-06-04T10:00:00+09:00"),
                        List.of(
                                new AiReviewReferenceBookSectionMapCandidateGenerator.PageText(3, "창세기"),
                                new AiReviewReferenceBookSectionMapCandidateGenerator.PageText(4, "창세기"),
                                new AiReviewReferenceBookSectionMapCandidateGenerator.PageText(8, "마태복음")
                        )
                );

        assertThat(document.sections()).extracting("bookCode")
                .containsExactly("GEN", "MAT");
        assertThat(document.summary().duplicateDetectionCount()).isEqualTo(1);
        assertThat(document.summary().missingBookCount()).isEqualTo(64);
        assertThat(document.summary().missingBooks()).extracting("bookCode")
                .contains("EXO", "REV");
    }

    @Test
    void ignoresDenseBookTitlePagesAsTableOfContentsCandidates() {
        AiReviewReferenceBookSectionMapCandidateWriter.CandidateDocument document =
                generator.documentFromPages(
                        "reference.pdf",
                        "sha256:source-hash",
                        OffsetDateTime.parse("2026-06-04T10:00:00+09:00"),
                        List.of(
                                new AiReviewReferenceBookSectionMapCandidateGenerator.PageText(5, """
                                        창세기
                                        출애굽기
                                        레위기
                                        민수기
                                        """),
                                new AiReviewReferenceBookSectionMapCandidateGenerator.PageText(20, "창세기"),
                                new AiReviewReferenceBookSectionMapCandidateGenerator.PageText(30, "출애굽기")
                        )
                );

        assertThat(document.sections()).extracting("bookCode")
                .containsExactly("GEN", "EXO");
        assertThat(document.sections().get(0).pageStart()).isEqualTo(20);
        assertThat(document.sections().get(1).pageStart()).isEqualTo(30);
    }

    @Test
    void createsCandidateDocumentEvenWhenResultIsIncomplete() {
        AiReviewReferenceBookSectionMapCandidateWriter.CandidateDocument document =
                generator.documentFromPages(
                        "reference.pdf",
                        "sha256:source-hash",
                        OffsetDateTime.parse("2026-06-04T10:00:00+09:00"),
                        List.of(new AiReviewReferenceBookSectionMapCandidateGenerator.PageText(1, "본문만 있는 페이지"))
                );

        assertThat(document.schemaVersion()).isEqualTo("ai-review-reference-book-section-map-candidate.v1");
        assertThat(document.sections()).isEmpty();
        assertThat(document.summary().missingBookCount()).isEqualTo(66);
    }
}
