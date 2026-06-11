package com.qtai.domain.ai.internal;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Component;

import com.qtai.common.exception.BusinessException;
import com.qtai.common.exception.ErrorCode;
import com.qtai.domain.bible.api.GetBibleVerseUseCase;
import com.qtai.domain.bible.api.dto.BibleVerseResponse;
import com.qtai.domain.qt.api.GetQtPassageContentContextUseCase;
import com.qtai.domain.qt.api.dto.QtPassageContentContext;
import com.qtai.external.llm.LlmClient;
import com.qtai.external.llm.dto.LlmCompletionRequest;
import com.qtai.external.llm.dto.LlmCompletionResponse;

@Component
class ExplanationGenerationJobHandler implements AiGenerationJobHandler {

    private static final int MAX_TOKENS = 2_000;
    private static final double TEMPERATURE = 0.2;
    private static final String SOURCE_LABEL = "QT-AI DeepSeek";

    private final GetQtPassageContentContextUseCase getQtPassageContentContextUseCase;
    private final GetBibleVerseUseCase getBibleVerseUseCase;
    private final CommentaryMaterialService commentaryMaterialService;
    private final AiPromptVersionRepository promptVersionRepository;
    private final LlmClient llmClient;
    private final ObjectMapper objectMapper;

    ExplanationGenerationJobHandler(
            GetQtPassageContentContextUseCase getQtPassageContentContextUseCase,
            GetBibleVerseUseCase getBibleVerseUseCase,
            CommentaryMaterialService commentaryMaterialService,
            AiPromptVersionRepository promptVersionRepository,
            LlmClient llmClient,
            ObjectMapper objectMapper
    ) {
        this.getQtPassageContentContextUseCase = getQtPassageContentContextUseCase;
        this.getBibleVerseUseCase = getBibleVerseUseCase;
        this.commentaryMaterialService = commentaryMaterialService;
        this.promptVersionRepository = promptVersionRepository;
        this.llmClient = llmClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public AiGenerationJobType jobType() {
        return AiGenerationJobType.EXPLANATION;
    }

    @Override
    public AiGeneratedAsset generate(AiGenerationJob job, OffsetDateTime createdAt) {
        AiPromptVersion promptVersion = promptVersion(job.getPromptVersionId());
        ExplanationInput input = input(job);
        LlmCompletionResponse response = llmClient.complete(new LlmCompletionRequest(
                null,
                systemPrompt(),
                userPrompt(input),
                MAX_TOKENS,
                TEMPERATURE
        ));
        String payloadJson = payloadJson(job, promptVersion, input, response);

        return AiGeneratedAsset.create(
                job.getId(),
                AiGeneratedAssetType.EXPLANATION,
                job.getTargetType(),
                job.getTargetId(),
                payloadJson,
                SOURCE_LABEL,
                createdAt
        );
    }

    private AiPromptVersion promptVersion(Long promptVersionId) {
        return promptVersionRepository.findById(promptVersionId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.INVALID_INPUT,
                        "PROMPT_VERSION_NOT_FOUND"
                ));
    }

    private ExplanationInput input(AiGenerationJob job) {
        if (job.getTargetType() == AiTargetType.QT_PASSAGE) {
            QtPassageContentContext context = getQtPassageContentContextUseCase.getContentContext(job.getTargetId());
            List<Long> verseIds = requireVerseIds(context.verseIds());
            return new ExplanationInput(
                    context.qtPassageId(),
                    context.qtDate(),
                    context.title(),
                    job.getTargetType(),
                    job.getTargetId(),
                    verseIds,
                    bibleVerses(verseIds),
                    commentaryMaterialService.findPromptContextByVerseIds(verseIds)
            );
        }
        if (job.getTargetType() == AiTargetType.BIBLE_VERSE) {
            List<Long> verseIds = List.of(requirePositive(job.getTargetId(), "targetId"));
            return new ExplanationInput(
                    null,
                    null,
                    "Single verse explanation",
                    job.getTargetType(),
                    job.getTargetId(),
                    verseIds,
                    bibleVerses(verseIds),
                    commentaryMaterialService.findPromptContextByVerseIds(verseIds)
            );
        }
        throw new BusinessException(ErrorCode.INVALID_INPUT, "EXPLANATION_TARGET_TYPE_UNSUPPORTED");
    }

    private List<BibleVerseResponse> bibleVerses(List<Long> verseIds) {
        List<BibleVerseResponse> verses = getBibleVerseUseCase.getVerses(verseIds);
        Map<Long, BibleVerseResponse> versesById = new LinkedHashMap<>();
        for (BibleVerseResponse verse : verses) {
            if (verse != null && verse.id() != null) {
                versesById.put(verse.id(), verse);
            }
        }

        return verseIds.stream()
                .map(verseId -> {
                    BibleVerseResponse verse = versesById.get(verseId);
                    if (verse == null) {
                        throw new BusinessException(ErrorCode.INVALID_INPUT, "BIBLE_VERSE_NOT_FOUND_FOR_AI_INPUT");
                    }
                    return verse;
                })
                .toList();
    }

    private String payloadJson(
            AiGenerationJob job,
            AiPromptVersion promptVersion,
            ExplanationInput input,
            LlmCompletionResponse response
    ) {
        JsonNode root = responseRoot(response.content());
        Set<Long> allowedVerseIds = new LinkedHashSet<>(input.verseIds());

        ObjectNode payload = objectMapper.createObjectNode();
        payload.set("explanations", validatedExplanations(root, allowedVerseIds));
        payload.set("glossaryTerms", validatedGlossaryTerms(root, allowedVerseIds));
        payload.put("promptVersionId", promptVersion.getId());
        payload.put("promptVersion", promptVersion.getVersion());
        payload.put("promptContentHash", promptVersion.getContentHash());
        payload.put("modelName", requireText(response.model(), "LLM_MODEL_NAME_MISSING"));
        payload.set("tokenUsage", tokenUsage(response));
        payload.set("sourceMetadata", sourceMetadata(job, input));

        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException exception) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "AI_PAYLOAD_SERIALIZATION_FAILED");
        }
    }

    private JsonNode responseRoot(String content) {
        try {
            JsonNode root = objectMapper.readTree(requireText(content, "LLM_RESPONSE_EMPTY"));
            if (!root.isObject()) {
                throw new BusinessException(ErrorCode.INVALID_INPUT, "LLM_RESPONSE_MUST_BE_OBJECT");
            }
            return root;
        } catch (JsonProcessingException exception) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "LLM_RESPONSE_INVALID_JSON");
        }
    }

    private ArrayNode validatedExplanations(JsonNode root, Set<Long> allowedVerseIds) {
        JsonNode explanations = requireArray(root, "explanations");
        if (explanations.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "LLM_RESPONSE_EXPLANATIONS_REQUIRED");
        }

        Set<Long> explanationVerseIds = new LinkedHashSet<>();
        ArrayNode sanitized = objectMapper.createArrayNode();
        for (JsonNode explanation : explanations) {
            long verseId = requireScopedVerseId(explanation, allowedVerseIds);
            if (!explanationVerseIds.add(verseId)) {
                throw new BusinessException(ErrorCode.INVALID_INPUT, "LLM_RESPONSE_DUPLICATE_EXPLANATION_VERSE_ID");
            }

            ObjectNode item = objectMapper.createObjectNode();
            item.put("verseId", verseId);
            item.put("summary", requireText(explanation.get("summary"), "summary"));
            item.put("explanation", requireText(explanation.get("explanation"), "explanation"));
            sanitized.add(item);
        }
        if (!explanationVerseIds.containsAll(allowedVerseIds)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "LLM_RESPONSE_MISSING_EXPLANATION_VERSE_ID");
        }
        return sanitized;
    }

    private ArrayNode validatedGlossaryTerms(JsonNode root, Set<Long> allowedVerseIds) {
        JsonNode glossaryTerms = requireArray(root, "glossaryTerms");
        ArrayNode sanitized = objectMapper.createArrayNode();
        for (JsonNode term : glossaryTerms) {
            ObjectNode item = objectMapper.createObjectNode();
            item.put("verseId", requireScopedVerseId(term, allowedVerseIds));
            item.put("term", requireText(term.get("term"), "term"));
            item.put("meaning", requireText(term.get("meaning"), "meaning"));
            sanitized.add(item);
        }
        return sanitized;
    }

    private JsonNode requireArray(JsonNode root, String fieldName) {
        JsonNode node = root.get(fieldName);
        if (node == null || !node.isArray()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "LLM_RESPONSE_" + fieldName + "_ARRAY_REQUIRED");
        }
        return node;
    }

    private long requireScopedVerseId(JsonNode node, Set<Long> allowedVerseIds) {
        JsonNode verseIdNode = node.get("verseId");
        if (verseIdNode == null || !verseIdNode.canConvertToLong()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "LLM_RESPONSE_VERSE_ID_REQUIRED");
        }

        long verseId = verseIdNode.asLong();
        if (!allowedVerseIds.contains(verseId)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "LLM_RESPONSE_VERSE_ID_OUT_OF_SCOPE");
        }
        return verseId;
    }

    private ObjectNode tokenUsage(LlmCompletionResponse response) {
        ObjectNode tokenUsage = objectMapper.createObjectNode();
        putNullable(tokenUsage, "promptTokens", response.promptTokens());
        putNullable(tokenUsage, "completionTokens", response.completionTokens());
        putNullable(tokenUsage, "totalTokens", response.totalTokens());
        return tokenUsage;
    }

    private ObjectNode sourceMetadata(AiGenerationJob job, ExplanationInput input) {
        ObjectNode sourceMetadata = objectMapper.createObjectNode();
        sourceMetadata.put("targetType", job.getTargetType().name());
        sourceMetadata.put("targetId", job.getTargetId());
        if (input.qtPassageId() != null) {
            sourceMetadata.put("qtPassageId", input.qtPassageId());
            sourceMetadata.put("qtDate", input.qtDate().toString());
            sourceMetadata.put("title", input.title());
        }

        ArrayNode verseIds = objectMapper.createArrayNode();
        ArrayNode verses = objectMapper.createArrayNode();
        for (BibleVerseResponse verse : input.verses()) {
            verseIds.add(verse.id());
            ObjectNode verseMetadata = objectMapper.createObjectNode();
            verseMetadata.put("verseId", verse.id());
            verseMetadata.put("bookCode", verse.bookCode());
            verseMetadata.put("chapterNo", verse.chapterNo());
            verseMetadata.put("verseNo", verse.verseNo());
            verses.add(verseMetadata);
        }
        sourceMetadata.set("verseIds", verseIds);
        sourceMetadata.set("verses", verses);
        putCommentaryMetadata(sourceMetadata, input.commentary());
        return sourceMetadata;
    }

    private void putCommentaryMetadata(ObjectNode sourceMetadata, CommentaryMaterialContext commentary) {
        if (commentary == null || !commentary.hasMaterials()) {
            sourceMetadata.putNull("commentarySource");
            sourceMetadata.putNull("sourceName");
            sourceMetadata.putNull("licenseLabel");
            sourceMetadata.putNull("copyrightNotice");
            sourceMetadata.set("commentaryMaterialIds", objectMapper.createArrayNode());
            sourceMetadata.putNull("commentaryVerseRange");
            return;
        }

        putNullable(sourceMetadata, "commentarySource", commentary.commentarySource());
        putNullable(sourceMetadata, "sourceName", commentary.sourceName());
        putNullable(sourceMetadata, "licenseLabel", commentary.licenseLabel());
        putNullable(sourceMetadata, "copyrightNotice", commentary.copyrightNotice());
        putNullable(sourceMetadata, "commentaryVerseRange", commentary.verseRange());
        ArrayNode materialIds = objectMapper.createArrayNode();
        commentary.commentaryMaterialIds().forEach(materialIds::add);
        sourceMetadata.set("commentaryMaterialIds", materialIds);
    }

    private String userPrompt(ExplanationInput input) {
        StringBuilder builder = new StringBuilder();
        builder.append("Create explanation JSON for the following Bible verses.\n");
        builder.append("Target type: ").append(input.targetType()).append('\n');
        builder.append("Target id: ").append(input.targetId()).append('\n');
        if (input.qtPassageId() != null) {
            builder.append("QT passage id: ").append(input.qtPassageId()).append('\n');
            builder.append("QT date: ").append(input.qtDate()).append('\n');
            builder.append("QT title: ").append(input.title()).append('\n');
        }
        builder.append("Verses:\n");
        for (BibleVerseResponse verse : input.verses()) {
            builder.append("- verseId=").append(verse.id())
                    .append(", bookCode=").append(verse.bookCode())
                    .append(", chapter=").append(verse.chapterNo())
                    .append(", verse=").append(verse.verseNo())
                    .append(", koreanText=").append(nullToEmpty(verse.koreanText()))
                    .append(", englishText=").append(nullToEmpty(verse.englishText()))
                    .append('\n');
        }
        if (input.commentary() != null && input.commentary().hasMaterials()) {
            builder.append("Commentary materials:\n");
            builder.append("Source: ").append(nullToEmpty(input.commentary().sourceName()))
                    .append(" (").append(nullToEmpty(input.commentary().licenseLabel())).append(")\n");
            for (CommentaryMaterialContext.MaterialExcerpt material : input.commentary().materials()) {
                builder.append("- materialId=").append(material.materialId())
                        .append(", refs=").append(material.refs())
                        .append(", title=").append(nullToEmpty(material.title()))
                        .append(", verseIds=").append(material.verseIds())
                        .append(", excerpt=").append(nullToEmpty(material.excerpt()))
                        .append('\n');
            }
        }
        return builder.toString();
    }

    private static String systemPrompt() {
        return """
                Return only a JSON object. The object must contain explanations[] and glossaryTerms[].
                Each explanation item must contain verseId, summary, and explanation.
                Each glossary term item must contain verseId, term, and meaning.
                Use only the provided verseIds and keep the tone calm, factual, and beginner-friendly.
                Do not include provider raw response, prompt text, validation reference text, secrets, or private data.
                """;
    }

    private static void putNullable(ObjectNode node, String fieldName, Integer value) {
        if (value == null) {
            node.putNull(fieldName);
            return;
        }
        node.put(fieldName, value);
    }

    private static void putNullable(ObjectNode node, String fieldName, String value) {
        if (value == null || value.isBlank()) {
            node.putNull(fieldName);
            return;
        }
        node.put(fieldName, value);
    }

    private static List<Long> requireVerseIds(List<Long> verseIds) {
        if (verseIds == null || verseIds.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "QT_PASSAGE_VERSE_IDS_REQUIRED");
        }
        LinkedHashSet<Long> uniqueIds = new LinkedHashSet<>();
        for (Long verseId : verseIds) {
            uniqueIds.add(requirePositive(verseId, "verseId"));
        }
        return List.copyOf(uniqueIds);
    }

    private static Long requirePositive(Long value, String fieldName) {
        if (value == null || value <= 0) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, fieldName + " must be positive");
        }
        return value;
    }

    private static String requireText(JsonNode node, String fieldName) {
        if (node == null || !node.isTextual()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, fieldName + " must be a non-blank string");
        }
        return requireText(node.asText(), fieldName);
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, fieldName + " must be a non-blank string");
        }
        return value;
    }

    private static String nullToEmpty(String value) {
        if (value == null) {
            return "";
        }
        return value;
    }

    private record ExplanationInput(
            Long qtPassageId,
            LocalDate qtDate,
            String title,
            AiTargetType targetType,
            Long targetId,
            List<Long> verseIds,
            List<BibleVerseResponse> verses,
            CommentaryMaterialContext commentary
    ) {
    }
}
