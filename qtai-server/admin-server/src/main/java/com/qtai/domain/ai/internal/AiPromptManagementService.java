package com.qtai.domain.ai.internal;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.qtai.common.exception.BusinessException;
import com.qtai.common.exception.ErrorCode;
import com.qtai.domain.ai.api.admin.prompt.ActivateAiPromptVersionUseCase;
import com.qtai.domain.ai.api.admin.prompt.CreateAiPromptVersionUseCase;
import com.qtai.domain.ai.api.admin.prompt.GetAiPromptVersionUseCase;
import com.qtai.domain.ai.api.admin.prompt.ListAiPromptVersionsUseCase;
import com.qtai.domain.ai.api.admin.prompt.RetireAiPromptVersionUseCase;
import com.qtai.domain.ai.api.admin.prompt.dto.AiPromptVersionListResponse;
import com.qtai.domain.ai.api.admin.prompt.dto.AiPromptVersionResponse;
import com.qtai.domain.ai.api.admin.prompt.dto.ChangeAiPromptVersionStatusCommand;
import com.qtai.domain.ai.api.admin.prompt.dto.CreateAiPromptVersionCommand;
import com.qtai.domain.ai.api.admin.prompt.dto.GetAiPromptVersionQuery;
import com.qtai.domain.ai.api.admin.prompt.dto.ListAiPromptVersionsQuery;
import com.qtai.domain.audit.api.WriteAuditLogUseCase;
import com.qtai.domain.audit.api.dto.AuditLogWriteRequest;

@Service
public class AiPromptManagementService implements
        ListAiPromptVersionsUseCase,
        GetAiPromptVersionUseCase,
        CreateAiPromptVersionUseCase,
        ActivateAiPromptVersionUseCase,
        RetireAiPromptVersionUseCase {

    private static final String SORT = "createdAt,desc,id,desc";
    private static final int MAX_PAGE_SIZE = 100;
    private static final int MAX_TOKENS_LIMIT = 20_000;
    private static final String ACTOR_TYPE_ADMIN = "ADMIN";
    private static final String TARGET_TYPE = "AI_PROMPT_VERSION";

    private final AiPromptVersionRepository promptVersionRepository;
    private final AiEvaluationRunRepository evaluationRunRepository;
    private final WriteAuditLogUseCase auditLogUseCase;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    @Autowired
    public AiPromptManagementService(
            AiPromptVersionRepository promptVersionRepository,
            AiEvaluationRunRepository evaluationRunRepository,
            WriteAuditLogUseCase auditLogUseCase,
            ObjectMapper objectMapper
    ) {
        this(promptVersionRepository, evaluationRunRepository, auditLogUseCase, objectMapper,
                Clock.systemDefaultZone());
    }

    AiPromptManagementService(
            AiPromptVersionRepository promptVersionRepository,
            AiEvaluationRunRepository evaluationRunRepository,
            WriteAuditLogUseCase auditLogUseCase,
            ObjectMapper objectMapper,
            Clock clock
    ) {
        this.promptVersionRepository = promptVersionRepository;
        this.evaluationRunRepository = evaluationRunRepository;
        this.auditLogUseCase = auditLogUseCase;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public AiPromptVersionListResponse listAiPromptVersions(ListAiPromptVersionsQuery query) {
        requireValidListQuery(query);
        requireAuthorizedReviewer(query.memberRole(), query.adminRole());

        AiPromptType promptType = parseEnum(AiPromptType.class, query.promptType(), "promptType");
        AiPromptVersionStatus status = parseEnum(AiPromptVersionStatus.class, query.status(), "status");
        PageRequest pageRequest = pageRequest(query.page(), query.size());
        Page<AiPromptVersion> page = findPromptPage(promptType, status, pageRequest);
        return new AiPromptVersionListResponse(
                page.getContent().stream().map(AiPromptManagementService::toResponse).toList(),
                query.page(),
                query.size(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast(),
                SORT
        );
    }

    @Override
    @Transactional(readOnly = true)
    public AiPromptVersionResponse getAiPromptVersion(GetAiPromptVersionQuery query) {
        requireValidGetQuery(query);
        requireAuthorizedReviewer(query.memberRole(), query.adminRole());
        return toResponse(findPromptVersion(query.promptVersionId()));
    }

    @Override
    @Transactional
    public AiPromptVersionResponse createAiPromptVersion(CreateAiPromptVersionCommand command) {
        requireValidCreateCommand(command);
        requireAuthorizedReviewer(command.memberRole(), command.adminRole());
        AiPromptType promptType = parsePromptTypeForManagedScope(command.promptType());
        if (promptVersionRepository.existsByPromptTypeAndVersion(promptType, command.version())) {
            throw new BusinessException(ErrorCode.DUPLICATE_RESOURCE);
        }

        OffsetDateTime now = OffsetDateTime.now(clock);
        AiPromptVersion promptVersion = AiPromptVersion.createDraft(
                promptType,
                command.version(),
                contentHash(command),
                AiPromptVersion.defaultSystemPrompt(),
                command.userPromptTemplate(),
                command.modelName(),
                command.temperature(),
                command.maxTokens(),
                command.description(),
                command.adminId(),
                now
        );
        try {
            AiPromptVersion saved = promptVersionRepository.save(promptVersion);
            writeAudit(command.adminId(), "PROMPT_CREATE", saved.getId(), null, snapshot(saved, now));
            return toResponse(saved);
        } catch (DataIntegrityViolationException exception) {
            throw new BusinessException(ErrorCode.DUPLICATE_RESOURCE);
        }
    }

    @Override
    @Transactional
    public AiPromptVersionResponse activateAiPromptVersion(ChangeAiPromptVersionStatusCommand command) {
        requireValidStatusCommand(command);
        requireAuthorizedReviewer(command.memberRole(), command.adminRole());

        AiPromptType promptType = findPromptType(command.promptVersionId());
        requireManagedScope(promptType);
        requirePassingEvaluationRun(command.promptVersionId());
        List<AiPromptVersion> lockedVersions = promptVersionRepository.findAllByPromptTypeForUpdate(promptType);
        AiPromptVersion target = lockedVersions.stream()
                .filter(promptVersion -> command.promptVersionId().equals(promptVersion.getId()))
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
        if (target.getStatus() != AiPromptVersionStatus.DRAFT) {
            throw new BusinessException(ErrorCode.INVALID_STATUS_TRANSITION);
        }

        OffsetDateTime now = OffsetDateTime.now(clock);
        for (AiPromptVersion active : lockedVersions) {
            if (active.getStatus() != AiPromptVersionStatus.ACTIVE) {
                continue;
            }
            String beforeJson = snapshot(active, now);
            active.retire(now);
            writeAudit(command.adminId(), "PROMPT_RETIRE", active.getId(), beforeJson, snapshot(active, now));
        }

        String beforeJson = snapshot(target, now);
        target.activate(now);
        writeAudit(command.adminId(), "PROMPT_ACTIVATE", target.getId(), beforeJson, snapshot(target, now));
        return toResponse(target);
    }

    @Override
    @Transactional
    public AiPromptVersionResponse retireAiPromptVersion(ChangeAiPromptVersionStatusCommand command) {
        requireValidStatusCommand(command);
        requireAuthorizedReviewer(command.memberRole(), command.adminRole());

        AiPromptVersion target = findPromptVersion(command.promptVersionId());
        requireManagedScope(target.getPromptType());
        OffsetDateTime now = OffsetDateTime.now(clock);
        String beforeJson = snapshot(target, now);
        target.retire(now);
        writeAudit(command.adminId(), "PROMPT_RETIRE", target.getId(), beforeJson, snapshot(target, now));
        return toResponse(target);
    }

    private Page<AiPromptVersion> findPromptPage(
            AiPromptType promptType,
            AiPromptVersionStatus status,
            PageRequest pageRequest
    ) {
        if (promptType != null && status != null) {
            return promptVersionRepository.findByPromptTypeAndStatus(promptType, status, pageRequest);
        }
        if (promptType != null) {
            return promptVersionRepository.findByPromptType(promptType, pageRequest);
        }
        if (status != null) {
            return promptVersionRepository.findByStatus(status, pageRequest);
        }
        return promptVersionRepository.findAll(pageRequest);
    }

    private AiPromptVersion findPromptVersion(Long promptVersionId) {
        return promptVersionRepository.findById(promptVersionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
    }

    private AiPromptType findPromptType(Long promptVersionId) {
        return promptVersionRepository.findPromptTypeById(promptVersionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
    }

    private void requirePassingEvaluationRun(Long promptVersionId) {
        AiEvaluationRun latestRun = evaluationRunRepository
                .findFirstByPromptVersionIdAndStatusOrderByFinishedAtDescIdDesc(
                        promptVersionId,
                        AiEvaluationRunStatus.SUCCEEDED
                )
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.INVALID_STATUS_TRANSITION,
                        "PROMPT_ACTIVATION_REQUIRES_SUCCESSFUL_EVALUATION_RUN"
                ));
        if (latestRun.getTotalCount() <= 0
                || latestRun.getFailedCount() > 0
                || latestRun.getNeedsReviewCount() > 0) {
            throw new BusinessException(
                    ErrorCode.INVALID_STATUS_TRANSITION,
                    "PROMPT_ACTIVATION_REQUIRES_ALL_EVALUATION_CASES_PASSED"
            );
        }
    }

    private String snapshot(AiPromptVersion promptVersion, OffsetDateTime eventAt) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("id", promptVersion.getId());
        payload.put("promptType", promptVersion.getPromptType().name());
        payload.put("version", promptVersion.getVersion());
        payload.put("contentHash", promptVersion.getContentHash());
        payload.put("status", promptVersion.getStatus().name());
        payload.put("timestamp", eventAt.toString());
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException exception) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "audit snapshot serialization failed");
        }
    }

    private void writeAudit(Long adminId, String actionType, Long targetId, String beforeJson, String afterJson) {
        auditLogUseCase.write(new AuditLogWriteRequest(
                null,
                ACTOR_TYPE_ADMIN,
                adminId,
                ACTOR_TYPE_ADMIN + ":" + adminId,
                actionType,
                TARGET_TYPE,
                targetId,
                beforeJson,
                afterJson
        ));
    }

    private static AiPromptVersionResponse toResponse(AiPromptVersion promptVersion) {
        return new AiPromptVersionResponse(
                promptVersion.getId(),
                promptVersion.getPromptType().name(),
                promptVersion.getVersion(),
                promptVersion.getContentHash(),
                promptVersion.getStatus().name(),
                promptVersion.getSystemPrompt(),
                promptVersion.getUserPromptTemplate(),
                promptVersion.getModelName(),
                promptVersion.getTemperature(),
                promptVersion.getMaxTokens(),
                promptVersion.getDescription(),
                promptVersion.getCreatedByAdminId(),
                promptVersion.getCreatedAt(),
                promptVersion.getActivatedAt(),
                promptVersion.getRetiredAt()
        );
    }

    private static String contentHash(CreateAiPromptVersionCommand command) {
        return contentHash(
                command.promptType(),
                command.version(),
                command.userPromptTemplate(),
                command.modelName(),
                command.temperature(),
                command.maxTokens()
        );
    }

    static String contentHash(
            String promptType,
            String version,
            String userPromptTemplate,
            String modelName,
            Double temperature,
            Integer maxTokens
    ) {
        String content = String.join("\n",
                promptType,
                version,
                AiPromptVersion.defaultSystemPrompt(),
                userPromptTemplate,
                nullToEmpty(modelName),
                String.valueOf(temperature == null ? AiPromptVersion.DEFAULT_TEMPERATURE : temperature),
                String.valueOf(maxTokens == null ? AiPromptVersion.DEFAULT_MAX_TOKENS : maxTokens)
        );
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(content.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "SHA-256 is not available");
        }
    }

    private static void requireAuthorizedReviewer(String memberRole, String adminRole) {
        if (!"ADMIN".equals(memberRole) || !("REVIEWER".equals(adminRole) || "SUPER_ADMIN".equals(adminRole))) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
    }

    private static void requireValidListQuery(ListAiPromptVersionsQuery query) {
        if (query == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "query must not be null");
        }
        requirePositive(query.adminId(), "adminId");
        requireText(query.memberRole(), "memberRole");
        requireText(query.adminRole(), "adminRole");
        requirePage(query.page(), query.size());
    }

    private static void requireValidGetQuery(GetAiPromptVersionQuery query) {
        if (query == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "query must not be null");
        }
        requirePositive(query.adminId(), "adminId");
        requireText(query.memberRole(), "memberRole");
        requireText(query.adminRole(), "adminRole");
        requirePositive(query.promptVersionId(), "promptVersionId");
    }

    private static void requireValidCreateCommand(CreateAiPromptVersionCommand command) {
        if (command == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "command must not be null");
        }
        requirePositive(command.adminId(), "adminId");
        requireText(command.memberRole(), "memberRole");
        requireText(command.adminRole(), "adminRole");
        requireText(command.promptType(), "promptType");
        requireText(command.version(), "version");
        requireText(command.userPromptTemplate(), "userPromptTemplate");
        if (command.temperature() != null && (command.temperature() < 0.0 || command.temperature() > 2.0)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "temperature must be between 0 and 2");
        }
        if (command.maxTokens() != null && (command.maxTokens() < 1 || command.maxTokens() > MAX_TOKENS_LIMIT)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "maxTokens must be between 1 and 20000");
        }
    }

    private static void requireValidStatusCommand(ChangeAiPromptVersionStatusCommand command) {
        if (command == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "command must not be null");
        }
        requirePositive(command.adminId(), "adminId");
        requireText(command.memberRole(), "memberRole");
        requireText(command.adminRole(), "adminRole");
        requirePositive(command.promptVersionId(), "promptVersionId");
    }

    private static void requireManagedScope(AiPromptType promptType) {
        if (promptType != AiPromptType.EXPLANATION) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "only EXPLANATION prompt versions are supported");
        }
    }

    private static AiPromptType parsePromptTypeForManagedScope(String value) {
        AiPromptType promptType = parseEnumRequired(AiPromptType.class, value, "promptType");
        requireManagedScope(promptType);
        return promptType;
    }

    private static void requirePage(int page, int size) {
        if (page < 0) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "page must not be negative");
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "size must be between 1 and 100");
        }
    }

    private static Long requirePositive(Long value, String fieldName) {
        if (value == null || value <= 0) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, fieldName + " must be positive");
        }
        return value;
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, fieldName + " must not be blank");
        }
        return value;
    }

    private static PageRequest pageRequest(int page, int size) {
        return PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")
                .and(Sort.by(Sort.Direction.DESC, "id")));
    }

    private static <E extends Enum<E>> E parseEnum(Class<E> enumType, String value, String fieldName) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return parseEnumRequired(enumType, value, fieldName);
    }

    private static <E extends Enum<E>> E parseEnumRequired(Class<E> enumType, String value, String fieldName) {
        try {
            return Enum.valueOf(enumType, requireText(value, fieldName));
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, fieldName + " is not supported");
        }
    }

    private static String nullToEmpty(String value) {
        if (value == null) {
            return "";
        }
        return value;
    }
}
