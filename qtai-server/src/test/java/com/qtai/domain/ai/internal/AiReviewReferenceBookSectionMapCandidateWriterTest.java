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

class AiReviewReferenceBookSectionMapCandidateWriterTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final AiReviewReferenceBookSectionMapCandidateWriter writer =
            new AiReviewReferenceBookSectionMapCandidateWriter(objectMapper);

    @TempDir
    Path tempDir;

    @Test
    void writesCandidateJsonWithRequiredSchemaAndSectionFields() throws Exception {
        Path candidateFile = tempDir.resolve("candidate.json");
        Path summaryFile = tempDir.resolve("summary.json");

        writer.write(candidateDocument(), candidateFile, summaryFile);

        JsonNode root = objectMapper.readTree(candidateFile.toFile());
        JsonNode section = root.path("sections").get(0);
        assertThat(root.path("schemaVersion").asText())
                .isEqualTo("ai-review-reference-book-section-map-candidate.v1");
        assertThat(root.path("sourceFileName").asText()).isEqualTo("reference.pdf");
        assertThat(root.path("summary").path("detectedSectionCount").asInt()).isEqualTo(1);
        assertThat(section.path("bookCode").asText()).isEqualTo("MAT");
        assertThat(section.path("bookName").asText()).isEqualTo("마태복음");
        assertThat(section.path("pageStart").asInt()).isEqualTo(20);
        assertThat(section.path("pageEnd").asInt()).isEqualTo(30);
        assertThat(section.path("detectedTitle").asText()).isEqualTo("마태복음");
        assertThat(section.path("confidence").asText()).isEqualTo("HIGH");
    }

    @Test
    void writesSummaryWithoutPdfTextOrExtractedBody() throws Exception {
        Path candidateFile = tempDir.resolve("candidate.json");
        Path summaryFile = tempDir.resolve("summary.json");

        writer.write(candidateDocument(), candidateFile, summaryFile);

        String summaryJson = Files.readString(summaryFile);
        assertThat(summaryJson).doesNotContain("referenceText");
        assertThat(summaryJson).doesNotContain("본문 원문");
        JsonNode summary = objectMapper.readTree(summaryJson);
        assertThat(summary.path("schemaVersion").asText())
                .isEqualTo("ai-review-reference-book-section-map-candidate.v1");
        assertThat(summary.path("summary").path("missingBookCount").asInt()).isEqualTo(1);
    }

    private static AiReviewReferenceBookSectionMapCandidateWriter.CandidateDocument candidateDocument() {
        return AiReviewReferenceBookSectionMapCandidateWriter.CandidateDocument.fromSections(
                "reference.pdf",
                "sha256:source-hash",
                OffsetDateTime.parse("2026-06-04T10:00:00+09:00"),
                List.of(new AiReviewReferenceBookSectionMapCandidateWriter.CandidateSection(
                        "MAT",
                        "마태복음",
                        20,
                        30,
                        "마태복음",
                        AiReviewReferenceBookSectionMapCandidateWriter.Confidence.HIGH,
                        List.of("EXACT_BOOK_TITLE")
                )),
                List.of(new AiReviewReferenceBookSectionMapCandidateWriter.MissingBook(
                        "GEN",
                        "창세기",
                        1
                )),
                0
        );
    }
}
