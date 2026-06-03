package com.qtai.domain.ai.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class AiReviewReferencePdfIndexDiagnosticsToolTest {

    @Test
    void parsesRequiredArgumentsWhenOutputsAreUnderBuildDirectory() {
        AiReviewReferencePdfIndexDiagnosticsTool.Arguments arguments =
                AiReviewReferencePdfIndexDiagnosticsTool.Arguments.parse(new String[]{
                        "--source", "../doc/reference.pdf",
                        "--output", "build/ai-review-reference/candidate.json",
                        "--summary", "build/ai-review-reference/summary.json"
                });

        assertThat(arguments.source().toString()).contains("reference.pdf");
        assertThat(arguments.output().toString()).contains("candidate.json");
        assertThat(arguments.summary().toString()).contains("summary.json");
    }

    @Test
    void rejectsMissingArguments() {
        assertThatThrownBy(() -> AiReviewReferencePdfIndexDiagnosticsTool.Arguments.parse(new String[]{
                "--source", "../doc/reference.pdf"
        })).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Usage:");
    }

    @Test
    void rejectsCandidateOutputOutsideBuildDirectory() {
        assertThatThrownBy(() -> AiReviewReferencePdfIndexDiagnosticsTool.Arguments.parse(new String[]{
                "--source", "../doc/reference.pdf",
                "--output", "../doc/reference-index-candidate.json",
                "--summary", "build/ai-review-reference/summary.json"
        })).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("AI_REVIEW_REFERENCE_PDF_OUTPUT_MUST_BE_UNDER_BUILD");
    }
}
