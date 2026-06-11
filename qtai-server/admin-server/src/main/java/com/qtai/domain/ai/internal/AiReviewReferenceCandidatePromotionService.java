package com.qtai.domain.ai.internal;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

class AiReviewReferenceCandidatePromotionService {

    static final String CANDIDATE_SCHEMA_VERSION = "ai-review-reference-index-candidate.v1";
    static final String OUTPUT_SCHEMA_VERSION = "ai-review-reference-index.v1";
    private static final String UNUSABLE = "UNUSABLE";

    private final ObjectMapper objectMapper;

    AiReviewReferenceCandidatePromotionService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper.copy()
                .findAndRegisterModules()
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .enable(SerializationFeature.INDENT_OUTPUT);
    }

    PromotionResult promote(Path candidatePath, AiReviewReferenceBookSectionMapReader.BookSectionMap sectionMap) {
        try {
            return promote(Files.readString(candidatePath), sectionMap);
        } catch (IOException exception) {
            throw new IllegalArgumentException("AI_REVIEW_REFERENCE_CANDIDATE_INDEX_READ_FAILED", exception);
        }
    }

    PromotionResult promote(String candidateJson, AiReviewReferenceBookSectionMapReader.BookSectionMap sectionMap) {
        JsonNode root = parseJson(candidateJson);
        String schemaVersion = requiredText(root, "schemaVersion");
        if (!CANDIDATE_SCHEMA_VERSION.equals(schemaVersion)) {
            throw new IllegalArgumentException("AI_REVIEW_REFERENCE_CANDIDATE_INDEX_SCHEMA_INVALID");
        }

        String sourceFileHash = requiredText(root, "sourceFileHash");
        if (!sectionMap.sourceFileHash().equals(sourceFileHash)) {
            throw new IllegalArgumentException("AI_REVIEW_REFERENCE_CANDIDATE_INDEX_HASH_MISMATCH");
        }

        JsonNode entriesNode = root.get("entries");
        if (entriesNode == null || !entriesNode.isArray()) {
            throw invalidCandidate();
        }

        List<PromotedEntry> promotedEntries = new ArrayList<>();
        int unusableEntryCount = 0;
        int unmappedEntryCount = 0;
        for (JsonNode entryNode : entriesNode) {
            String status = requiredText(entryNode.path("quality"), "status");
            if (UNUSABLE.equals(status)) {
                unusableEntryCount++;
                continue;
            }

            int pageStart = requiredPositiveInt(entryNode, "pageStart");
            AiReviewReferenceBookSectionMapReader.BookSection section = sectionMap.sectionForPage(pageStart);
            if (section == null) {
                unmappedEntryCount++;
                continue;
            }

            String detectedHeading = requiredText(entryNode, "detectedHeading");
            promotedEntries.add(new PromotedEntry(
                    section.bookCode(),
                    requiredPositiveInt(entryNode, "chapterStart"),
                    requiredPositiveInt(entryNode, "verseStart"),
                    requiredPositiveInt(entryNode, "chapterEnd"),
                    requiredPositiveInt(entryNode, "verseEnd"),
                    section.bookName() + " " + detectedHeading,
                    requiredText(entryNode, "referenceText"),
                    requiredText(entryNode, "referenceHash")
            ));
        }

        PromotedIndex promotedIndex = new PromotedIndex(
                OUTPUT_SCHEMA_VERSION,
                sourceFileHash,
                OffsetDateTime.now(ZoneOffset.UTC),
                promotedEntries
        );
        PromotionSummary summary = new PromotionSummary(
                root.path("entries").size(),
                promotedEntries.size(),
                unusableEntryCount,
                unmappedEntryCount
        );
        return new PromotionResult(promotedIndex, summary);
    }

    void write(PromotionResult result, Path outputPath, Path summaryPath) {
        try {
            Files.createDirectories(outputPath.toAbsolutePath().getParent());
            Files.createDirectories(summaryPath.toAbsolutePath().getParent());
            objectMapper.writeValue(outputPath.toFile(), result.promotedIndex());
            objectMapper.writeValue(summaryPath.toFile(), result.summary());
        } catch (IOException exception) {
            throw new IllegalStateException("AI_REVIEW_REFERENCE_PROMOTION_WRITE_FAILED", exception);
        }
    }

    String summaryJson(PromotionResult result) {
        try {
            return objectMapper.writeValueAsString(result.summary());
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("AI_REVIEW_REFERENCE_PROMOTION_SUMMARY_WRITE_FAILED", exception);
        }
    }

    private JsonNode parseJson(String json) {
        try {
            JsonNode root = objectMapper.readTree(json);
            if (root == null || !root.isObject()) {
                throw invalidCandidate();
            }
            return root;
        } catch (JsonProcessingException exception) {
            throw invalidCandidate();
        }
    }

    private static String requiredText(JsonNode node, String fieldName) {
        JsonNode field = node == null ? null : node.get(fieldName);
        if (field == null || !field.isTextual() || field.asText().isBlank()) {
            throw invalidCandidate();
        }
        return field.asText();
    }

    private static int requiredPositiveInt(JsonNode node, String fieldName) {
        JsonNode field = node == null ? null : node.get(fieldName);
        if (field == null || !field.canConvertToInt()) {
            throw invalidCandidate();
        }
        int value = field.asInt();
        if (value < 1) {
            throw invalidCandidate();
        }
        return value;
    }

    private static IllegalArgumentException invalidCandidate() {
        return new IllegalArgumentException("AI_REVIEW_REFERENCE_CANDIDATE_INDEX_INVALID");
    }

    record PromotionResult(
            PromotedIndex promotedIndex,
            PromotionSummary summary
    ) {
    }

    record PromotedIndex(
            String schemaVersion,
            String sourceFileHash,
            OffsetDateTime generatedAt,
            List<PromotedEntry> entries
    ) {

        PromotedIndex {
            entries = List.copyOf(entries);
        }
    }

    record PromotedEntry(
            String bookCode,
            int chapterStart,
            int verseStart,
            int chapterEnd,
            int verseEnd,
            String referenceRangeLabel,
            String referenceText,
            String referenceHash
    ) {
    }

    record PromotionSummary(
            int candidateEntryCount,
            int promotedEntryCount,
            int unusableEntryCount,
            int unmappedEntryCount
    ) {
    }
}
