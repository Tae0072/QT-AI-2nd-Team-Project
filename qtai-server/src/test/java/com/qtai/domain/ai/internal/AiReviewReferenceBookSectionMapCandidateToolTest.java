package com.qtai.domain.ai.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class AiReviewReferenceBookSectionMapCandidateToolTest {

    @Test
    void parsesRequiredArgumentsWhenOutputsAreUnderBuildDirectory() {
        AiReviewReferenceBookSectionMapCandidateTool.Arguments arguments =
                AiReviewReferenceBookSectionMapCandidateTool.Arguments.parse(new String[]{
                        "--source", "../doc/reference.pdf",
                        "--output", "build/ai-review-reference/book-section-map-candidate.json",
                        "--summary", "build/ai-review-reference/book-section-map-summary.json"
                });

        assertThat(arguments.source().toString()).contains("reference.pdf");
        assertThat(arguments.output().toString()).contains("book-section-map-candidate.json");
        assertThat(arguments.summary().toString()).contains("book-section-map-summary.json");
    }

    @Test
    void rejectsMissingArguments() {
        assertThatThrownBy(() -> AiReviewReferenceBookSectionMapCandidateTool.Arguments.parse(new String[]{
                "--source", "../doc/reference.pdf"
        })).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Usage:");
    }

    @Test
    void rejectsOutputOutsideBuildDirectory() {
        assertThatThrownBy(() -> AiReviewReferenceBookSectionMapCandidateTool.Arguments.parse(new String[]{
                "--source", "../doc/reference.pdf",
                "--output", "../doc/book-section-map-candidate.json",
                "--summary", "build/ai-review-reference/book-section-map-summary.json"
        })).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("AI_REVIEW_REFERENCE_BOOK_SECTION_MAP_OUTPUT_MUST_BE_UNDER_BUILD");
    }
}
