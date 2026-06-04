package com.qtai.domain.ai.internal;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

class AiReviewReferencePdfIndexCandidateWriterTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final AiReviewReferencePdfIndexCandidateWriter writer =
            new AiReviewReferencePdfIndexCandidateWriter(objectMapper);

    @TempDir
    Path tempDir;

    @Test
    void writesCandidateJsonWithRequiredSchemaAndEntryFields() throws Exception {
        Path candidateFile = tempDir.resolve("candidate.json");
        Path summaryFile = tempDir.resolve("summary.json");
        AiReviewReferencePdfIndexCandidateWriter.CandidateDocument document = candidateDocument();

        writer.write(document, candidateFile, summaryFile);

        JsonNode root = objectMapper.readTree(candidateFile.toFile());
        JsonNode entry = root.path("entries").get(0);
        assertThat(root.path("schemaVersion").asText()).isEqualTo("ai-review-reference-index-candidate.v1");
        assertThat(root.path("sourceFileName").asText()).isEqualTo("reference.pdf");
        assertThat(root.path("sourceFileHash").asText()).isEqualTo("sha256:source-hash");
        assertThat(root.path("qualitySummary").path("totalEntryCount").asInt()).isEqualTo(1);
        assertThat(entry.path("pageStart").asInt()).isEqualTo(10);
        assertThat(entry.path("detectedHeading").asText()).isEqualTo("19:11-15");
        assertThat(entry.path("bookCode").isNull()).isTrue();
        assertThat(entry.path("referenceRangeLabel").asText()).isEqualTo("19:11-15");
        assertThat(entry.path("referenceText").asText()).isEqualTo("참조 본문 후보");
        assertThat(entry.path("referenceHash").asText()).isEqualTo(
                AiReviewReferencePdfIndexCandidateWriter.sha256Hex("참조 본문 후보")
        );
    }

    @Test
    void writesSummaryWithoutReferenceText() throws Exception {
        Path candidateFile = tempDir.resolve("candidate.json");
        Path summaryFile = tempDir.resolve("summary.json");

        writer.write(candidateDocument(), candidateFile, summaryFile);

        String summaryJson = Files.readString(summaryFile);
        assertThat(summaryJson).doesNotContain("referenceText");
        assertThat(summaryJson).doesNotContain("참조 본문 후보");
        JsonNode summary = objectMapper.readTree(summaryJson);
        assertThat(summary.path("schemaVersion").asText()).isEqualTo("ai-review-reference-index-candidate.v1");
        assertThat(summary.path("qualitySummary").path("needsReviewEntryCount").asInt()).isEqualTo(1);
    }

    private static AiReviewReferencePdfIndexCandidateWriter.CandidateDocument candidateDocument() {
        AiReviewReferenceTextQualityAnalyzer.QualityResult quality =
                new AiReviewReferenceTextQualityAnalyzer.QualityResult(
                        AiReviewReferenceTextQualityAnalyzer.QualityStatus.NEEDS_REVIEW,
                        0,
                        0.0,
                        0.0,
                        0.8,
                        8,
                        List.of("BOOK_CODE_NOT_DETECTED")
                );
        AiReviewReferencePdfIndexCandidateWriter.CandidateEntry entry =
                new AiReviewReferencePdfIndexCandidateWriter.CandidateEntry(
                        10,
                        "19:11-15",
                        null,
                        19,
                        11,
                        19,
                        15,
                        "19:11-15",
                        "참조 본문 후보",
                        AiReviewReferencePdfIndexCandidateWriter.sha256Hex("참조 본문 후보"),
                        quality
                );
        return AiReviewReferencePdfIndexCandidateWriter.CandidateDocument.fromEntries(
                "reference.pdf",
                "sha256:source-hash",
                OffsetDateTime.parse("2026-06-04T10:00:00+09:00"),
                List.of(entry)
        );
    }
}
