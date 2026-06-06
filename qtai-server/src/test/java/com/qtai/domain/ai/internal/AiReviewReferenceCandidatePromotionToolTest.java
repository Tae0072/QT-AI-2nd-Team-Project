package com.qtai.domain.ai.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class AiReviewReferenceCandidatePromotionToolTest {

    @Test
    void parsesRequiredArgumentsWhenOutputsAreUnderBuildDirectory() {
        AiReviewReferenceCandidatePromotionTool.Arguments arguments =
                AiReviewReferenceCandidatePromotionTool.Arguments.parse(new String[]{
                        "--candidate", "build/ai-review-reference/candidate.json",
                        "--book-section-map", "build/ai-review-reference/book-map.json",
                        "--output", "build/ai-review-reference/reference-index.json",
                        "--summary", "build/ai-review-reference/reference-index-promotion-summary.json"
                });

        assertThat(arguments.candidate().toString()).contains("candidate.json");
        assertThat(arguments.bookSectionMap().toString()).contains("book-map.json");
        assertThat(arguments.output().toString()).contains("reference-index.json");
        assertThat(arguments.summary().toString()).contains("promotion-summary.json");
    }

    @Test
    void rejectsMissingArguments() {
        assertThatThrownBy(() -> AiReviewReferenceCandidatePromotionTool.Arguments.parse(new String[]{
                "--candidate", "build/ai-review-reference/candidate.json"
        })).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Usage:");
    }

    @Test
    void rejectsOutputOutsideBuildDirectory() {
        assertThatThrownBy(() -> AiReviewReferenceCandidatePromotionTool.Arguments.parse(new String[]{
                "--candidate", "build/ai-review-reference/candidate.json",
                "--book-section-map", "build/ai-review-reference/book-map.json",
                "--output", "../doc/reference-index.json",
                "--summary", "build/ai-review-reference/reference-index-promotion-summary.json"
        })).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("AI_REVIEW_REFERENCE_PROMOTION_OUTPUT_MUST_BE_UNDER_BUILD");
    }
}
