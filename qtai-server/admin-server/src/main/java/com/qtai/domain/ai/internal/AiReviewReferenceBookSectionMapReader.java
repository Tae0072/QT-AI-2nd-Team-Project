package com.qtai.domain.ai.internal;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

class AiReviewReferenceBookSectionMapReader {

    static final String SCHEMA_VERSION = "ai-review-reference-book-section-map.v1";

    private final ObjectMapper objectMapper;

    AiReviewReferenceBookSectionMapReader(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    BookSectionMap read(Path mapPath, String expectedSourceFileHash) {
        try {
            return parse(Files.readString(mapPath), expectedSourceFileHash);
        } catch (IOException exception) {
            throw new IllegalArgumentException("AI_REVIEW_REFERENCE_BOOK_SECTION_MAP_READ_FAILED", exception);
        }
    }

    BookSectionMap parse(String json, String expectedSourceFileHash) {
        JsonNode root = parseJson(json);
        requireEquals(requiredText(root, "schemaVersion"), SCHEMA_VERSION,
                "AI_REVIEW_REFERENCE_BOOK_SECTION_MAP_SCHEMA_INVALID");
        String sourceFileHash = requiredText(root, "sourceFileHash");
        requireEquals(sourceFileHash, expectedSourceFileHash,
                "AI_REVIEW_REFERENCE_BOOK_SECTION_MAP_HASH_MISMATCH");

        JsonNode sectionsNode = root.get("sections");
        if (sectionsNode == null || !sectionsNode.isArray() || sectionsNode.isEmpty()) {
            throw invalidMap();
        }

        List<BookSection> sections = new ArrayList<>();
        for (JsonNode sectionNode : sectionsNode) {
            sections.add(new BookSection(
                    requiredText(sectionNode, "bookCode"),
                    requiredText(sectionNode, "bookName"),
                    requiredPositiveInt(sectionNode, "pageStart"),
                    requiredPositiveInt(sectionNode, "pageEnd")
            ));
        }
        validateSections(sections);
        return new BookSectionMap(
                requiredText(root, "sourceFileName"),
                sourceFileHash,
                sections
        );
    }

    private JsonNode parseJson(String json) {
        try {
            JsonNode root = objectMapper.readTree(json);
            if (root == null || !root.isObject()) {
                throw invalidMap();
            }
            return root;
        } catch (JsonProcessingException exception) {
            throw invalidMap();
        }
    }

    private static void validateSections(List<BookSection> sections) {
        List<BookSection> sorted = sections.stream()
                .sorted(Comparator.comparingInt(BookSection::pageStart))
                .toList();
        int previousEnd = 0;
        for (BookSection section : sorted) {
            if (section.pageStart() > section.pageEnd()) {
                throw new IllegalArgumentException("AI_REVIEW_REFERENCE_BOOK_SECTION_MAP_RANGE_INVALID");
            }
            if (section.pageStart() <= previousEnd) {
                throw new IllegalArgumentException("AI_REVIEW_REFERENCE_BOOK_SECTION_MAP_RANGE_OVERLAPPED");
            }
            previousEnd = section.pageEnd();
        }
    }

    private static String requiredText(JsonNode node, String fieldName) {
        JsonNode field = node == null ? null : node.get(fieldName);
        if (field == null || !field.isTextual() || field.asText().isBlank()) {
            throw invalidMap();
        }
        return field.asText();
    }

    private static int requiredPositiveInt(JsonNode node, String fieldName) {
        JsonNode field = node == null ? null : node.get(fieldName);
        if (field == null || !field.canConvertToInt()) {
            throw invalidMap();
        }
        int value = field.asInt();
        if (value < 1) {
            throw invalidMap();
        }
        return value;
    }

    private static void requireEquals(String actual, String expected, String message) {
        if (!expected.equals(actual)) {
            throw new IllegalArgumentException(message);
        }
    }

    private static IllegalArgumentException invalidMap() {
        return new IllegalArgumentException("AI_REVIEW_REFERENCE_BOOK_SECTION_MAP_INVALID");
    }

    record BookSectionMap(
            String sourceFileName,
            String sourceFileHash,
            List<BookSection> sections
    ) {

        BookSectionMap {
            sections = List.copyOf(sections);
        }

        BookSection sectionForPage(int pageStart) {
            for (BookSection section : sections) {
                if (pageStart >= section.pageStart() && pageStart <= section.pageEnd()) {
                    return section;
                }
            }
            return null;
        }
    }

    record BookSection(
            String bookCode,
            String bookName,
            int pageStart,
            int pageEnd
    ) {
    }
}
