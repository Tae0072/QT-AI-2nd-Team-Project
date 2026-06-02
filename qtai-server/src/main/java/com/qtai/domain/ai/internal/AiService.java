package com.qtai.domain.ai.internal;

import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.qtai.common.exception.BusinessException;
import com.qtai.common.exception.ErrorCode;
import com.qtai.domain.ai.api.CreateAiGenerationJobUseCase;
import com.qtai.domain.ai.api.RegenerateAiAssetUseCase;
import com.qtai.domain.ai.api.dto.CreateAiGenerationJobCommand;
import com.qtai.domain.ai.api.dto.CreateAiGenerationJobResult;
import com.qtai.domain.ai.api.dto.RegenerateAiAssetCommand;
import com.qtai.domain.ai.api.dto.RegenerateAiAssetResult;
import com.qtai.domain.audit.api.WriteAuditLogUseCase;
import com.qtai.domain.audit.api.dto.AuditLogWriteRequest;

@Service
public class AiService implements CreateAiGenerationJobUseCase, RegenerateAiAssetUseCase {

    private static final String ACTIVE_JOB_UNIQUE_CONSTRAINT = "uk_ai_generation_jobs_active_target_prompt";
    private static final String ACTIVE_TARGET_UNIQUE_CONSTRAINT = "uk_ai_generation_jobs_active_target";
    private static final String ACTOR_TYPE_ADMIN = "ADMIN";
    private static final String ACTION_AI_REGENERATE_REQUEST = "AI_REGENERATE_REQUEST";
    private static final String TARGET_TYPE_AI_GENERATED_ASSET = "AI_GENERATED_ASSET";

    private static final List<AiGenerationJobStatus> ACTIVE_GENERATION_STATUSES = List.of(
            AiGenerationJobStatus.QUEUED,
            AiGenerationJobStatus.RUNNING
    );

    private final AiGenerationJobRepository generationJobRepository;
    private final AiGeneratedAssetRepository generatedAssetRepository;
    private final AiPromptVersionRepository promptVersionRepository;
    private final WriteAuditLogUseCase auditLogUseCase;
    private final ObjectMapper objectMapper;

    @Autowired
    public AiService(
            AiGenerationJobRepository generationJobRepository,
            AiGeneratedAssetRepository generatedAssetRepository,
            AiPromptVersionRepository promptVersionRepository,
            WriteAuditLogUseCase auditLogUseCase,
            ObjectMapper objectMapper
    ) {
        this.generationJobRepository = generationJobRepository;
        this.generatedAssetRepository = generatedAssetRepository;
        this.promptVersionRepository = promptVersionRepository;
        this.auditLogUseCase = auditLogUseCase;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public CreateAiGenerationJobResult createAiGenerationJob(CreateAiGenerationJobCommand command) {
        requireValidCommand(command);

        AiGenerationJobType jobType = parseJobType(command.jobType());
        AiTargetType targetType = parseTargetType(command.targetType());
        AiPromptVersion promptVersionEntity = requireUsablePromptVersion(command.promptVersionId(), jobType);
        if (generationJobRepository.existsByJobTypeAndTargetTypeAndTargetIdAndStatusIn(
                jobType,
                targetType,
                command.targetId(),
                ACTIVE_GENERATION_STATUSES
        )) {
            throw new BusinessException(
                    ErrorCode.INVALID_STATUS_TRANSITION,
                    "같은 대상의 진행 중 AI 생성 작업이 있어 새 작업을 생성할 수 없습니다."
            );
        }

        AiGenerationJob job = AiGenerationJob.queue(
                jobType,
                targetType,
                command.targetId(),
                promptVersionEntity.getId(),
                command.requestedAt()
        );
        AiGenerationJob savedJob = saveQueuedJob(job);
        return new CreateAiGenerationJobResult(
                savedJob.getId(),
                savedJob.getStatus().name()
        );
    }

    @Override
    @Transactional
    public RegenerateAiAssetResult regenerateAiAsset(RegenerateAiAssetCommand command) {
        requireValidCommand(command);
        // Controller 인증 우회나 내부 호출 오용을 막기 위한 도메인 경계의 2차 권한 검증이다.
        requireAuthorizedReviewer(command.memberRole(), command.adminRole());

        AiGeneratedAsset asset = generatedAssetRepository.findById(command.assetId())
                .orElseThrow(() -> new BusinessException(ErrorCode.AI_ASSET_NOT_FOUND));
        requireRegeneratableStatus(asset);

        AiGenerationJobType jobType = jobTypeOf(asset.getAssetType());
        AiPromptVersion promptVersionEntity = requireUsablePromptVersion(command.promptVersionId(), jobType);
        if (generationJobRepository.existsByJobTypeAndTargetTypeAndTargetIdAndStatusIn(
                jobType,
                asset.getTargetType(),
                asset.getTargetId(),
                ACTIVE_GENERATION_STATUSES
        )) {
            throw new BusinessException(
                    ErrorCode.INVALID_STATUS_TRANSITION,
                    "같은 대상의 진행 중 AI 생성 작업이 있어 재생성을 요청할 수 없습니다."
            );
        }

        AiGenerationJob job = AiGenerationJob.queue(
                jobType,
                asset.getTargetType(),
                asset.getTargetId(),
                promptVersionEntity.getId(),
                command.requestedAt()
        );
        AiGenerationJob savedJob = saveQueuedJob(job);
        writeRegenerateAudit(command, asset, savedJob);

        return new RegenerateAiAssetResult(
                savedJob.getId(),
                savedJob.getStatus().name(),
                savedJob.getCreatedAt()
        );
    }

    private AiGenerationJob saveQueuedJob(AiGenerationJob job) {
        try {
            return generationJobRepository.saveAndFlush(job);
        } catch (DataIntegrityViolationException exception) {
            if (!isActiveJobUniqueConstraintViolation(exception)) {
                throw exception;
            }
            throw new BusinessException(
                    ErrorCode.INVALID_STATUS_TRANSITION,
                    "같은 대상의 진행 중 AI 생성 작업이 있어 새 작업을 생성할 수 없습니다."
            );
        }
    }

    private static void requireAuthorizedReviewer(String memberRole, String adminRole) {
        if (!"ADMIN".equals(memberRole) || !("REVIEWER".equals(adminRole) || "SUPER_ADMIN".equals(adminRole))) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
    }

    private static void requireValidCommand(CreateAiGenerationJobCommand command) {
        if (command == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "command must not be null");
        }
        requireText(command.jobType(), "jobType");
        requireText(command.targetType(), "targetType");
        requirePositive(command.targetId(), "targetId");
        requirePositive(command.promptVersionId(), "promptVersionId");
        requireText(command.requestedBy(), "requestedBy");
        if (command.requestedAt() == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "requestedAt must not be null");
        }
    }

    private static void requireValidCommand(RegenerateAiAssetCommand command) {
        if (command == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "command must not be null");
        }
        requirePositive(command.adminId(), "adminId");
        requirePositive(command.assetId(), "assetId");
        requireText(command.memberRole(), "memberRole");
        requireText(command.adminRole(), "adminRole");
        requirePositive(command.promptVersionId(), "promptVersionId");
        requireText(command.reason(), "reason");
        if (command.requestedAt() == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "requestedAt must not be null");
        }
    }

    private static void requirePositive(Long value, String fieldName) {
        if (value == null || value <= 0) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, fieldName + " must be positive");
        }
    }

    private static void requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, fieldName + " must not be blank");
        }
    }

    private static boolean isActiveJobUniqueConstraintViolation(DataIntegrityViolationException exception) {
        Throwable mostSpecificCause = exception.getMostSpecificCause();
        return containsActiveJobUniqueConstraint(exception.getMessage())
                || containsActiveJobUniqueConstraint(mostSpecificCause.getMessage());
    }

    private static boolean containsActiveJobUniqueConstraint(String message) {
        if (message == null) {
            return false;
        }
        String lowerCaseMessage = message.toLowerCase(Locale.ROOT);
        return lowerCaseMessage.contains(ACTIVE_JOB_UNIQUE_CONSTRAINT.toLowerCase(Locale.ROOT))
                || lowerCaseMessage.contains(ACTIVE_TARGET_UNIQUE_CONSTRAINT.toLowerCase(Locale.ROOT));
    }

    private static AiGenerationJobType parseJobType(String value) {
        try {
            return AiGenerationJobType.valueOf(value);
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "jobType is not supported");
        }
    }

    private static AiTargetType parseTargetType(String value) {
        try {
            return AiTargetType.valueOf(value);
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "targetType is not supported");
        }
    }

    private AiPromptVersion requireUsablePromptVersion(Long promptVersionId, AiGenerationJobType jobType) {
        AiPromptVersion promptVersionEntity = promptVersionRepository.findById(promptVersionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_INPUT, "promptVersionId is not found"));
        if (promptVersionEntity.getStatus() != AiPromptVersionStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "promptVersionId is not active");
        }
        AiPromptType expectedPromptType = promptTypeOf(jobType);
        if (promptVersionEntity.getPromptType() != expectedPromptType) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "promptVersionId does not match jobType");
        }
        return promptVersionEntity;
    }

    private static AiPromptType promptTypeOf(AiGenerationJobType jobType) {
        return switch (jobType) {
            case EXPLANATION -> AiPromptType.EXPLANATION;
            case SIMULATOR -> AiPromptType.SIMULATOR;
            case QA -> AiPromptType.QA;
            case SUMMARY, GLOSSARY -> throw new BusinessException(
                    ErrorCode.INVALID_INPUT,
                    "jobType is not supported by ai_prompt_versions.prompt_type"
            );
        };
    }

    private static void requireRegeneratableStatus(AiGeneratedAsset asset) {
        if (asset.getStatus() == AiGeneratedAssetStatus.REJECTED || asset.getStatus() == AiGeneratedAssetStatus.HIDDEN) {
            return;
        }
        throw new BusinessException(
                ErrorCode.INVALID_STATUS_TRANSITION,
                "REJECTED 또는 HIDDEN 상태의 AI 산출물만 재생성을 요청할 수 있습니다."
        );
    }

    private static AiGenerationJobType jobTypeOf(AiGeneratedAssetType assetType) {
        return switch (assetType) {
            case EXPLANATION -> AiGenerationJobType.EXPLANATION;
            case SUMMARY -> AiGenerationJobType.SUMMARY;
            case GLOSSARY -> AiGenerationJobType.GLOSSARY;
            case SIMULATOR -> AiGenerationJobType.SIMULATOR;
            case QA_RESPONSE -> AiGenerationJobType.QA;
        };
    }

    private void writeRegenerateAudit(
            RegenerateAiAssetCommand command,
            AiGeneratedAsset asset,
            AiGenerationJob savedJob
    ) {
        auditLogUseCase.write(new AuditLogWriteRequest(
                null,
                ACTOR_TYPE_ADMIN,
                command.adminId(),
                ACTOR_TYPE_ADMIN + ":" + command.adminId(),
                ACTION_AI_REGENERATE_REQUEST,
                TARGET_TYPE_AI_GENERATED_ASSET,
                command.assetId(),
                assetSnapshot(command.assetId(), asset),
                jobSnapshot(savedJob)
        ));
    }

    private String assetSnapshot(Long assetId, AiGeneratedAsset asset) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("id", assetId);
        payload.put("assetType", asset.getAssetType().name());
        payload.put("status", asset.getStatus().name());
        payload.put("targetType", asset.getTargetType().name());
        payload.put("targetId", asset.getTargetId());
        return toAuditJson(payload);
    }

    private String jobSnapshot(AiGenerationJob job) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("id", job.getId());
        payload.put("status", job.getStatus().name());
        payload.put("jobType", job.getJobType().name());
        payload.put("targetType", job.getTargetType().name());
        payload.put("targetId", job.getTargetId());
        payload.put("promptVersionId", job.getPromptVersionId());
        payload.put("requestedAt", DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(job.getCreatedAt()));
        return toAuditJson(payload);
    }

    private String toAuditJson(Map<String, Object> payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException exception) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "audit snapshot serialization failed");
        }
    }
}
