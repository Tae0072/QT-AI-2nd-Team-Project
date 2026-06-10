package com.qtai.domain.ai.internal;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qtai.common.exception.BusinessException;
import com.qtai.common.exception.ErrorCode;

@Component
class AiReviewReferenceExcerptSelector {

    static final int MAX_EXCERPT_COUNT = 3;
    static final int MAX_REFERENCE_TEXT_LENGTH = 1_200;
    static final String ASSET_VERSE_METADATA_NOT_FOUND = "AI_REVIEW_ASSET_VERSE_METADATA_NOT_FOUND";

    private final ObjectMapper objectMapper;

    AiReviewReferenceExcerptSelector(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    List<SelectedExcerpt> select(
            String assetPayloadJson,
            AiReviewReferenceIndexReader.ReferenceIndex referenceIndex
    ) {
        List<AssetVerse> assetVerses = assetVerses(assetPayloadJson);
        List<SelectedExcerpt> selected = new ArrayList<>();
        for (AiReviewReferenceIndexReader.ReferenceIndexEntry entry : referenceIndex.entries()) {
            if (selected.size() >= MAX_EXCERPT_COUNT) {
                break;
            }
            if (matchesAnyVerse(entry, assetVerses)) {
                selected.add(SelectedExcerpt.from(entry));
            }
        }
        return selected;
    }

    private List<AssetVerse> assetVerses(String assetPayloadJson) {
        try {
            JsonNode root = objectMapper.readTree(assetPayloadJson);
            JsonNode versesNode = root == null
                    ? null
                    : root.path("sourceMetadata").path("verses");
            if (versesNode == null || !versesNode.isArray() || versesNode.isEmpty()) {
                throw missingVerseMetadata();
            }
            List<AssetVerse> verses = new ArrayList<>();
            for (JsonNode verseNode : versesNode) {
                verses.add(new AssetVerse(
                        requiredText(verseNode, "bookCode"),
                        requiredPositiveInt(verseNode, "chapterNo"),
                        requiredPositiveInt(verseNode, "verseNo")
                ));
            }
            return verses;
        } catch (JsonProcessingException exception) {
            throw missingVerseMetadata();
        }
    }

    private static boolean matchesAnyVerse(
            AiReviewReferenceIndexReader.ReferenceIndexEntry entry,
            List<AssetVerse> assetVerses
    ) {
        for (AssetVerse assetVerse : assetVerses) {
            if (entry.bookCode().equals(assetVerse.bookCode())
                    && compare(assetVerse.chapterNo(), assetVerse.verseNo(), entry.chapterStart(), entry.verseStart()) >= 0
                    && compare(assetVerse.chapterNo(), assetVerse.verseNo(), entry.chapterEnd(), entry.verseEnd()) <= 0) {
                return true;
            }
        }
        return false;
    }

    private static int compare(int leftChapter, int leftVerse, int rightChapter, int rightVerse) {
        int chapterCompare = Integer.compare(leftChapter, rightChapter);
        if (chapterCompare != 0) {
            return chapterCompare;
        }
        return Integer.compare(leftVerse, rightVerse);
    }

    private static String requiredText(JsonNode node, String fieldName) {
        JsonNode field = node == null ? null : node.get(fieldName);
        if (field == null || !field.isTextual() || field.asText().isBlank()) {
            throw missingVerseMetadata();
        }
        return field.asText();
    }

    private static int requiredPositiveInt(JsonNode node, String fieldName) {
        JsonNode field = node == null ? null : node.get(fieldName);
        if (field == null || !field.canConvertToInt()) {
            throw missingVerseMetadata();
        }
        int value = field.asInt();
        if (value < 1) {
            throw missingVerseMetadata();
        }
        return value;
    }

    private static String truncateReferenceText(String referenceText) {
        if (referenceText.length() <= MAX_REFERENCE_TEXT_LENGTH) {
            return referenceText;
        }
        return referenceText.substring(0, MAX_REFERENCE_TEXT_LENGTH);
    }

    private static BusinessException missingVerseMetadata() {
        return new BusinessException(ErrorCode.INVALID_INPUT, ASSET_VERSE_METADATA_NOT_FOUND);
    }

    record SelectedExcerpt(
            String bookCode,
            int chapterStart,
            int verseStart,
            int chapterEnd,
            int verseEnd,
            String referenceRangeLabel,
            String referenceText,
            String referenceHash
    ) {

        private static SelectedExcerpt from(AiReviewReferenceIndexReader.ReferenceIndexEntry entry) {
            return new SelectedExcerpt(
                    entry.bookCode(),
                    entry.chapterStart(),
                    entry.verseStart(),
                    entry.chapterEnd(),
                    entry.verseEnd(),
                    entry.referenceRangeLabel(),
                    truncateReferenceText(entry.referenceText()),
                    entry.referenceHash()
            );
        }
    }

    private record AssetVerse(
            String bookCode,
            int chapterNo,
            int verseNo
    ) {
    }
}
