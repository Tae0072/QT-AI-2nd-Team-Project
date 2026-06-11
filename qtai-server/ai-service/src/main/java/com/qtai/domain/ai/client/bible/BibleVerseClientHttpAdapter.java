package com.qtai.domain.ai.client.bible;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qtai.domain.ai.client.AiClientException;
import com.qtai.domain.ai.client.AiClientException.FailureCode;
import com.qtai.domain.ai.client.http.AiClientProperties;
import com.qtai.domain.ai.client.http.AiHttpSupport;

@Component("aiBibleVerseClientHttpAdapter")
@ConditionalOnProperty(name = "qtai.ai.client.mode", havingValue = "http")
@ConditionalOnMissingBean(BibleVerseClient.class)
public class BibleVerseClientHttpAdapter implements BibleVerseClient {

    private static final String DOWNSTREAM = "bible";
    private static final String VERSE_PATH = "/api/v1/system/bible/verses/%d";
    private static final String BATCH_PATH = "/api/v1/system/bible/verses:batch";
    private static final String RANGE_PATH = "/api/v1/system/bible/verses";

    private final AiHttpSupport http;
    private final JavaType verseType;
    private final JavaType batchType;
    private final JavaType rangeType;

    public BibleVerseClientHttpAdapter(ObjectMapper objectMapper, AiClientProperties properties) {
        this.http = new AiHttpSupport(objectMapper, properties, properties.getBible(), DOWNSTREAM);
        this.verseType = objectMapper.getTypeFactory().constructType(BibleVerseResult.class);
        this.batchType = objectMapper.getTypeFactory().constructType(BibleVerseBatchResult.class);
        this.rangeType = objectMapper.getTypeFactory().constructType(BibleVerseRangeResult.class);
    }

    @Override
    public BibleVerseResult getVerse(Long verseId) {
        if (verseId == null) {
            throw validationFailure("verseId must not be null");
        }
        return http.get(VERSE_PATH.formatted(verseId), Map.of(), verseType);
    }

    @Override
    public List<BibleVerseResult> getVersesByIds(List<Long> verseIds) {
        if (verseIds == null || verseIds.isEmpty() || verseIds.stream().anyMatch(Objects::isNull)) {
            throw validationFailure("verseIds must not be null, empty, or contain null");
        }
        BibleVerseBatchResult result = http.post(BATCH_PATH, new BibleVerseBatchRequest(verseIds), batchType, false);
        if (result.verses() == null) {
            throw new AiClientException(FailureCode.RESPONSE_MAPPING_FAILED, DOWNSTREAM, "response verses is missing");
        }
        return result.verses();
    }

    @Override
    public BibleVerseRangeResult getVersesInRange(String bibleBook, int chapter, Integer startVerse, Integer endVerse) {
        if (!hasText(bibleBook)) {
            throw validationFailure("bibleBook must not be blank");
        }
        if (chapter < 1) {
            throw validationFailure("chapter must be positive");
        }
        Map<String, Object> queryParameters = new LinkedHashMap<>();
        queryParameters.put("book", bibleBook);
        queryParameters.put("chapter", chapter);
        if (startVerse != null) {
            queryParameters.put("startVerse", startVerse);
        }
        if (endVerse != null) {
            queryParameters.put("endVerse", endVerse);
        }
        return http.get(RANGE_PATH, queryParameters, rangeType);
    }

    private static AiClientException validationFailure(String message) {
        return new AiClientException(FailureCode.VALIDATION_FAILED, DOWNSTREAM, message);
    }

    private static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private record BibleVerseBatchRequest(List<Long> verseIds) {
    }

    private record BibleVerseBatchResult(List<BibleVerseResult> verses) {
    }
}
