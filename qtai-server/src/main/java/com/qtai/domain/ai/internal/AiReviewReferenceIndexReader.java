package com.qtai.domain.ai.internal;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qtai.common.exception.BusinessException;
import com.qtai.common.exception.ErrorCode;

@Component
class AiReviewReferenceIndexReader {

    private static final String SCHEMA_VERSION = "ai-review-reference-index.v1";
    private static final String READ_FAILED = "AI_REVIEW_REFERENCE_INDEX_READ_FAILED";
    private static final String INVALID_INDEX = "AI_REVIEW_REFERENCE_INDEX_INVALID";
    private static final String INVALID_SCHEMA = "AI_REVIEW_REFERENCE_INDEX_SCHEMA_INVALID";
    private static final String HASH_MISMATCH = "AI_REVIEW_REFERENCE_INDEX_HASH_MISMATCH";

    private final RestrictedStorageUriResolver uriResolver;
    private final ObjectMapper objectMapper;

    AiReviewReferenceIndexReader(
            RestrictedStorageUriResolver uriResolver,
            ObjectMapper objectMapper
    ) {
        this.uriResolver = uriResolver;
        this.objectMapper = objectMapper;
    }

    ReferenceIndex read(String indexStorageUri, String expectedSourceFileHash) {
        requireText(expectedSourceFileHash);
        Path indexPath = uriResolver.resolve(indexStorageUri);
        String json = readJson(indexPath);
        JsonNode root = parseJson(json);

        String schemaVersion = requiredText(root, "schemaVersion");
        if (!SCHEMA_VERSION.equals(schemaVersion)) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, INVALID_SCHEMA);
        }

        String sourceFileHash = requiredText(root, "sourceFileHash");
        if (!expectedSourceFileHash.equals(sourceFileHash)) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, HASH_MISMATCH);
        }

        OffsetDateTime generatedAt = parseGeneratedAt(root.get("generatedAt"));
        List<ReferenceIndexEntry> entries = parseEntries(root.get("entries"));
        return new ReferenceIndex(schemaVersion, sourceFileHash, generatedAt, entries);
    }

    private static String readJson(Path indexPath) {
        try {
            return Files.readString(indexPath, StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, READ_FAILED);
        }
    }

    private JsonNode parseJson(String json) {
        try {
            JsonNode root = objectMapper.readTree(json);
            if (root == null || !root.isObject()) {
                throw invalidIndex();
            }
            return root;
        } catch (JsonProcessingException exception) {
            throw invalidIndex();
        }
    }

    private static List<ReferenceIndexEntry> parseEntries(JsonNode entriesNode) {
        if (entriesNode == null || !entriesNode.isArray() || entriesNode.isEmpty()) {
            throw invalidIndex();
        }

        List<ReferenceIndexEntry> entries = new ArrayList<>();
        for (JsonNode entryNode : entriesNode) {
            if (!entryNode.isObject()) {
                throw invalidIndex();
            }
            entries.add(new ReferenceIndexEntry(
                    requiredText(entryNode, "bookCode"),
                    requiredPositiveInt(entryNode, "chapterStart"),
                    requiredPositiveInt(entryNode, "verseStart"),
                    requiredPositiveInt(entryNode, "chapterEnd"),
                    requiredPositiveInt(entryNode, "verseEnd"),
                    requiredText(entryNode, "referenceRangeLabel"),
                    requiredText(entryNode, "referenceText"),
                    requiredText(entryNode, "referenceHash")
            ));
        }
        return entries;
    }

    private static String requiredText(JsonNode node, String fieldName) {
        if (node == null || node.get(fieldName) == null || !node.get(fieldName).isTextual()
                || node.get(fieldName).asText().isBlank()) {
            throw invalidIndex();
        }
        return node.get(fieldName).asText();
    }

    private static int requiredPositiveInt(JsonNode node, String fieldName) {
        if (node == null || node.get(fieldName) == null || !node.get(fieldName).canConvertToInt()) {
            throw invalidIndex();
        }
        int value = node.get(fieldName).asInt();
        if (value < 1) {
            throw invalidIndex();
        }
        return value;
    }

    private static OffsetDateTime parseGeneratedAt(JsonNode generatedAtNode) {
        if (generatedAtNode == null) {
            throw invalidIndex();
        }
        if (generatedAtNode.isTextual()) {
            return parseGeneratedAtText(generatedAtNode.asText());
        }
        if (generatedAtNode.isNumber()) {
            return parseGeneratedAtEpochSeconds(generatedAtNode.decimalValue());
        }
        throw invalidIndex();
    }

    private static OffsetDateTime parseGeneratedAtText(String generatedAt) {
        try {
            return OffsetDateTime.parse(generatedAt);
        } catch (DateTimeParseException exception) {
            throw invalidIndex();
        }
    }

    private static OffsetDateTime parseGeneratedAtEpochSeconds(BigDecimal epochSeconds) {
        try {
            long seconds = epochSeconds.longValue();
            int nanos = epochSeconds
                    .subtract(BigDecimal.valueOf(seconds))
                    .movePointRight(9)
                    .intValue();
            return OffsetDateTime.ofInstant(Instant.ofEpochSecond(seconds, nanos), ZoneOffset.UTC);
        } catch (ArithmeticException exception) {
            throw invalidIndex();
        }
    }

    private static void requireText(String value) {
        if (value == null || value.isBlank()) {
            throw invalidIndex();
        }
    }

    private static BusinessException invalidIndex() {
        return new BusinessException(ErrorCode.INTERNAL_ERROR, INVALID_INDEX);
    }

    record ReferenceIndex(
            String schemaVersion,
            String sourceFileHash,
            OffsetDateTime generatedAt,
            List<ReferenceIndexEntry> entries
    ) {

        ReferenceIndex {
            entries = List.copyOf(entries);
        }
    }

    record ReferenceIndexEntry(
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
}
