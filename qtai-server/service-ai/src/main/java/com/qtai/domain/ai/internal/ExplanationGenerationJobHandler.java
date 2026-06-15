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
        GeneratedExplanation generated = generateForEvaluation(promptVersion, job.getTargetType(), job.getTargetId());

        return AiGeneratedAsset.create(
                job.getId(),
                AiGeneratedAssetType.EXPLANATION,
                job.getTargetType(),
                job.getTargetId(),
                generated.payloadJson(),
                SOURCE_LABEL,
                createdAt
        );
    }

    GeneratedExplanation generateForEvaluation(AiPromptVersion promptVersion, AiTargetType targetType, Long targetId) {
        if (promptVersion.getPromptType() != AiPromptType.EXPLANATION) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "PROMPT_VERSION_TYPE_MISMATCH");
        }
        ExplanationInput input = input(targetType, targetId);
        LlmCompletionResponse response = llmClient.complete(new LlmCompletionRequest(
                promptVersion.getModelName(),
                AiPromptVersion.defaultSystemPrompt(),
                userPrompt(promptVersion, input),
                promptVersion.getMaxTokens(),
                promptVersion.getTemperature()
        ));
        return new GeneratedExplanation(
                payloadJson(targetType, targetId, promptVersion, input, response),
                response.model()
        );
    }

    private AiPromptVersion promptVersion(Long promptVersionId) {
        return promptVersionRepository.findById(promptVersionId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.INVALID_INPUT,
                        "PROMPT_VERSION_NOT_FOUND"
                ));
    }

    private ExplanationInput input(AiTargetType targetType, Long targetId) {
        if (targetType == AiTargetType.QT_PASSAGE) {
            QtPassageContentContext context = getQtPassageContentContextUseCase.getContentContext(targetId);
            List<Long> verseIds = requireVerseIds(context.verseIds());
            return new ExplanationInput(
                    context.qtPassageId(),
                    context.qtDate(),
                    context.title(),
                    targetType,
                    targetId,
                    verseIds,
                    bibleVerses(verseIds),
                    commentaryMaterialService.findPromptContextByVerseIds(verseIds)
            );
        }
        if (targetType == AiTargetType.BIBLE_VERSE) {
            List<Long> verseIds = List.of(requirePositive(targetId, "targetId"));
            return new ExplanationInput(
                    null,
                    null,
                    "Single verse explanation",
                    targetType,
                    targetId,
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
            AiTargetType targetType,
            Long targetId,
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
        payload.set("sourceMetadata", sourceMetadata(targetType, targetId, input));

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

    private ObjectNode sourceMetadata(AiTargetType targetType, Long targetId, ExplanationInput input) {
        ObjectNode sourceMetadata = objectMapper.createObjectNode();
        sourceMetadata.put("targetType", targetType.name());
        sourceMetadata.put("targetId", targetId);
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

    private String userPrompt(AiPromptVersion promptVersion, ExplanationInput input) {
        String instruction = promptVersion.getUserPromptTemplate();
        if (hasLegacyPlaceholder(instruction)) {
            return legacyUserPrompt(instruction, input);
        }
        return naturalInstructionPrompt(instruction, input);
    }

    private String naturalInstructionPrompt(String instruction, ExplanationInput input) {
        StringBuilder builder = new StringBuilder();
        builder.append("다음 성경 구절에 대한 해설 JSON을 생성하세요.\n");
        builder.append("대상 유형: ").append(input.targetType().name()).append('\n');
        builder.append("대상 ID: ").append(input.targetId()).append('\n');
        builder.append('\n');

        String qtPassageBlock = koreanQtPassageBlock(input);
        if (!qtPassageBlock.isBlank()) {
            builder.append(qtPassageBlock).append('\n');
        }

        builder.append("구절 목록:\n");
        builder.append(koreanVersesBlock(input));

        String commentaryBlock = koreanCommentaryBlock(input);
        if (!commentaryBlock.isBlank()) {
            builder.append('\n').append(commentaryBlock);
        }

        builder.append('\n');
        builder.append("추가 생성 지시사항:\n");
        builder.append(instruction.strip()).append('\n');
        return builder.toString();
    }

    private String legacyUserPrompt(String template, ExplanationInput input) {
        return template
                .replace("{{targetType}}", input.targetType().name())
                .replace("{{targetId}}", String.valueOf(input.targetId()))
                .replace("{{qtPassageBlock}}", legacyQtPassageBlock(input))
                .replace("{{versesBlock}}", legacyVersesBlock(input))
                .replace("{{commentaryBlock}}", legacyCommentaryBlock(input));
    }

    private static boolean hasLegacyPlaceholder(String prompt) {
        return prompt.contains("{{targetType}}")
                || prompt.contains("{{targetId}}")
                || prompt.contains("{{qtPassageBlock}}")
                || prompt.contains("{{versesBlock}}")
                || prompt.contains("{{commentaryBlock}}");
    }

    private String koreanQtPassageBlock(ExplanationInput input) {
        if (input.qtPassageId() == null) {
            return "";
        }
        return "QT 본문 정보:\n"
                + "- QT 본문 ID: " + input.qtPassageId() + '\n'
                + "- QT 날짜: " + input.qtDate() + '\n'
                + "- QT 제목: " + input.title() + '\n';
    }

    private String koreanVersesBlock(ExplanationInput input) {
        StringBuilder builder = new StringBuilder();
        for (BibleVerseResponse verse : input.verses()) {
            builder.append("- verseId=").append(verse.id())
                    .append(", 성경책 코드=").append(verse.bookCode())
                    .append(", 장=").append(verse.chapterNo())
                    .append(", 절=").append(verse.verseNo())
                    .append(", 한글 본문=").append(nullToEmpty(verse.koreanText()))
                    .append(", 영어 본문=").append(nullToEmpty(verse.englishText()))
                    .append('\n');
        }
        return builder.toString();
    }

    private String koreanCommentaryBlock(ExplanationInput input) {
        StringBuilder builder = new StringBuilder();
        if (input.commentary() != null && input.commentary().hasMaterials()) {
            builder.append("참고 해설 자료:\n");
            builder.append("출처: ").append(nullToEmpty(input.commentary().sourceName()))
                    .append(" (").append(nullToEmpty(input.commentary().licenseLabel())).append(")\n");
            for (CommentaryMaterialContext.MaterialExcerpt material : input.commentary().materials()) {
                builder.append("- materialId=").append(material.materialId())
                        .append(", refs=").append(material.refs())
                        .append(", 제목=").append(nullToEmpty(material.title()))
                        .append(", verseIds=").append(material.verseIds())
                        .append(", 발췌=").append(nullToEmpty(material.excerpt()))
                        .append('\n');
            }
        }
        return builder.toString();
    }

    private String legacyQtPassageBlock(ExplanationInput input) {
        if (input.qtPassageId() == null) {
            return "";
        }
        return "QT passage id: " + input.qtPassageId() + '\n'
                + "QT date: " + input.qtDate() + '\n'
                + "QT title: " + input.title() + '\n';
    }

    private String legacyVersesBlock(ExplanationInput input) {
        StringBuilder builder = new StringBuilder();
        for (BibleVerseResponse verse : input.verses()) {
            builder.append("- verseId=").append(verse.id())
                    .append(", bookCode=").append(verse.bookCode())
                    .append(", chapter=").append(verse.chapterNo())
                    .append(", verse=").append(verse.verseNo())
                    .append(", koreanText=").append(nullToEmpty(verse.koreanText()))
                    .append(", englishText=").append(nullToEmpty(verse.englishText()))
                    .append('\n');
        }
        return builder.toString();
    }

    private String legacyCommentaryBlock(ExplanationInput input) {
        StringBuilder builder = new StringBuilder();
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

    private static String nullToEmpty(Long value) {
        if (value == null) {
            return "";
        }
        return String.valueOf(value);
    }

    record GeneratedExplanation(String payloadJson, String modelName) {
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
