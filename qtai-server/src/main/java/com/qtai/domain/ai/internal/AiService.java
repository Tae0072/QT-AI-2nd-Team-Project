package com.qtai.domain.ai.internal;

import java.util.List;
import java.util.Locale;

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

@Service
public class AiService implements CreateAiGenerationJobUseCase, RegenerateAiAssetUseCase {

    private static final String ACTIVE_JOB_UNIQUE_CONSTRAINT = "uk_ai_generation_jobs_active_target_prompt";

    private static final List<AiGenerationJobStatus> ACTIVE_GENERATION_STATUSES = List.of(
            AiGenerationJobStatus.QUEUED,
            AiGenerationJobStatus.RUNNING
    );

    private final AiGenerationJobRepository generationJobRepository;
    private final AiGeneratedAssetRepository generatedAssetRepository;

    public AiService(
            AiGenerationJobRepository generationJobRepository,
            AiGeneratedAssetRepository generatedAssetRepository
    ) {
        this.generationJobRepository = generationJobRepository;
        this.generatedAssetRepository = generatedAssetRepository;
    }

    @Override
    @Transactional
    public CreateAiGenerationJobResult createAiGenerationJob(CreateAiGenerationJobCommand command) {
        requireValidCommand(command);

        AiGenerationJobType jobType = parseJobType(command.jobType());
        AiTargetType targetType = parseTargetType(command.targetType());
        if (generationJobRepository.existsByJobTypeAndTargetTypeAndTargetIdAndPromptVersionAndStatusIn(
                jobType,
                targetType,
                command.targetId(),
                command.promptVersion(),
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
                command.promptVersion(),
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
        String promptVersion = asset.getPromptVersion();
        if (generationJobRepository.existsByJobTypeAndTargetTypeAndTargetIdAndPromptVersionAndStatusIn(
                jobType,
                asset.getTargetType(),
                asset.getTargetId(),
                promptVersion,
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
                promptVersion,
                command.requestedAt()
        );
        AiGenerationJob savedJob = saveQueuedJob(job);
        // TODO: audit 도메인 소유자가 WriteAuditLogUseCase 계약을 확정하면 AI_REGENERATE_REQUEST 기록을 연결한다.

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
        requireText(command.promptVersion(), "promptVersion");
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
        return message.toLowerCase(Locale.ROOT)
                .contains(ACTIVE_JOB_UNIQUE_CONSTRAINT.toLowerCase(Locale.ROOT));
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
}
