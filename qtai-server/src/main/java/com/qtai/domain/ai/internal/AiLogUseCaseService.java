package com.qtai.domain.ai.internal;

import java.time.OffsetDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.qtai.common.exception.BusinessException;
import com.qtai.common.exception.ErrorCode;
import com.qtai.domain.ai.api.RegisterAiGeneratedAssetUseCase;
import com.qtai.domain.ai.api.RegisterAiValidationLogUseCase;
import com.qtai.domain.ai.api.dto.RegisterAiGeneratedAssetCommand;
import com.qtai.domain.ai.api.dto.RegisterAiGeneratedAssetResult;
import com.qtai.domain.ai.api.dto.RegisterAiValidationLogCommand;
import com.qtai.domain.ai.api.dto.RegisterAiValidationLogResult;

@Service
class AiLogUseCaseService implements RegisterAiGeneratedAssetUseCase, RegisterAiValidationLogUseCase {

    private final AiLogService aiLogService;

    AiLogUseCaseService(AiLogService aiLogService) {
        this.aiLogService = aiLogService;
    }

    @Override
    @Transactional
    public RegisterAiGeneratedAssetResult registerAiGeneratedAsset(RegisterAiGeneratedAssetCommand command) {
        requireValidCommand(command);
        AiGeneratedAsset asset = aiLogService.registerGeneratedAsset(
                command.generationJobId(),
                parseEnum(AiGeneratedAssetType.class, command.assetType(), "assetType"),
                parseEnum(AiTargetType.class, command.targetType(), "targetType"),
                command.targetId(),
                command.payloadJson(),
                command.sourceLabel(),
                command.createdAt()
        );

        return new RegisterAiGeneratedAssetResult(asset.getId(), asset.getStatus().name());
    }

    @Override
    @Transactional
    public RegisterAiValidationLogResult registerAiValidationLog(RegisterAiValidationLogCommand command) {
        requireValidCommand(command);
        AiValidationResult validationResult = parseEnum(AiValidationResult.class, command.result(), "result");
        AiValidationLog log = aiLogService.registerValidationLog(
                command.assetId(),
                command.validationReferenceJobId(),
                command.layer(),
                validationResult,
                parseEnum(AiValidationReviewerType.class, command.reviewerType(), "reviewerType"),
                command.checklistVersionId(),
                command.checklistJson(),
                command.errorMessage(),
                command.createdAt()
        );

        return new RegisterAiValidationLogResult(
                log.getId(),
                log.getResult().name(),
                assetStatusAfter(validationResult).name()
        );
    }

    private static void requireValidCommand(RegisterAiGeneratedAssetCommand command) {
        if (command == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "command must not be null");
        }
        requirePositive(command.generationJobId(), "generationJobId");
        requireText(command.assetType(), "assetType");
        requireText(command.targetType(), "targetType");
        requirePositive(command.targetId(), "targetId");
        requireText(command.payloadJson(), "payloadJson");
        requireCreatedAt(command.createdAt());
    }

    private static void requireValidCommand(RegisterAiValidationLogCommand command) {
        if (command == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "command must not be null");
        }
        requirePositive(command.assetId(), "assetId");
        requirePositiveWhenPresent(command.validationReferenceJobId(), "validationReferenceJobId");
        if (command.layer() < 1) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "layer must be greater than zero");
        }
        requireText(command.result(), "result");
        requireText(command.reviewerType(), "reviewerType");
        requirePositive(command.checklistVersionId(), "checklistVersionId");
        requireCreatedAt(command.createdAt());
    }

    private static void requirePositive(Long value, String fieldName) {
        if (value == null || value <= 0) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, fieldName + " must be positive");
        }
    }

    private static void requirePositiveWhenPresent(Long value, String fieldName) {
        if (value != null && value <= 0) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, fieldName + " must be positive");
        }
    }

    private static void requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, fieldName + " must not be blank");
        }
    }

    private static void requireCreatedAt(OffsetDateTime createdAt) {
        if (createdAt == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "createdAt must not be null");
        }
    }

    private static <E extends Enum<E>> E parseEnum(Class<E> enumType, String value, String fieldName) {
        try {
            return Enum.valueOf(enumType, value);
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, fieldName + " is not supported");
        }
    }

    private static AiGeneratedAssetStatus assetStatusAfter(AiValidationResult result) {
        if (result == AiValidationResult.REJECTED) {
            return AiGeneratedAssetStatus.REJECTED;
        }
        return AiGeneratedAssetStatus.VALIDATING;
    }
}
