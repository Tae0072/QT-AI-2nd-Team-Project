package com.qtai.domain.ai.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.qtai.common.exception.BusinessException;
import com.qtai.common.exception.ErrorCode;
import com.qtai.domain.ai.api.generation.dto.RegisterAiGeneratedAssetCommand;
import com.qtai.domain.ai.api.generation.dto.RegisterAiGeneratedAssetResult;
import com.qtai.domain.ai.api.validation.dto.RegisterAiValidationLogCommand;
import com.qtai.domain.ai.api.validation.dto.RegisterAiValidationLogResult;

class AiLogUseCaseServiceTest {

    private AiLogService aiLogService;
    private AiLogUseCaseService aiLogUseCaseService;

    @BeforeEach
    void setUp() {
        aiLogService = org.mockito.Mockito.mock(AiLogService.class);
        aiLogUseCaseService = new AiLogUseCaseService(aiLogService);
    }

    @Test
    void generatedAssetCommandDelegatesToAiLogServiceAndReturnsAssetStatus() {
        OffsetDateTime createdAt = OffsetDateTime.parse("2026-05-26T10:30:00+09:00");
        AiGeneratedAsset savedAsset = org.mockito.Mockito.mock(AiGeneratedAsset.class);
        when(savedAsset.getId()).thenReturn(500L);
        when(savedAsset.getStatus()).thenReturn(AiGeneratedAssetStatus.VALIDATING);
        when(aiLogService.registerGeneratedAsset(
                101L,
                AiGeneratedAssetType.EXPLANATION,
                AiTargetType.QT_PASSAGE,
                35L,
                "{\"summary\":\"寃利??湲??댁꽕\"}",
                "QT-AI curated content",
                createdAt
        )).thenReturn(savedAsset);

        RegisterAiGeneratedAssetResult result = aiLogUseCaseService.registerAiGeneratedAsset(
                new RegisterAiGeneratedAssetCommand(
                        101L,
                        "EXPLANATION",
                        "QT_PASSAGE",
                        35L,
                        "{\"summary\":\"寃利??湲??댁꽕\"}",
                        "QT-AI curated content",
                        createdAt
                )
        );

        assertThat(result.assetId()).isEqualTo(500L);
        assertThat(result.status()).isEqualTo("VALIDATING");
        verify(aiLogService).registerGeneratedAsset(
                101L,
                AiGeneratedAssetType.EXPLANATION,
                AiTargetType.QT_PASSAGE,
                35L,
                "{\"summary\":\"寃利??湲??댁꽕\"}",
                "QT-AI curated content",
                createdAt
        );
    }

    @Test
    void validationLogCommandDelegatesWithValidationReferenceJobIdAndReturnsAssetStatus() {
        OffsetDateTime createdAt = OffsetDateTime.parse("2026-05-26T11:30:00+09:00");
        AiValidationLog savedLog = org.mockito.Mockito.mock(AiValidationLog.class);
        when(savedLog.getId()).thenReturn(700L);
        when(savedLog.getResult()).thenReturn(AiValidationResult.REJECTED);
        when(aiLogService.registerValidationLog(
                500L,
                33L,
                2,
                AiValidationResult.REJECTED,
                AiValidationReviewerType.AUTO,
                4L,
                "{\"rules\":[]}",
                "POLICY_VIOLATION",
                createdAt
        )).thenReturn(savedLog);

        RegisterAiValidationLogResult result = aiLogUseCaseService.registerAiValidationLog(
                new RegisterAiValidationLogCommand(
                        500L,
                        33L,
                        2,
                        "REJECTED",
                        "AUTO",
                        4L,
                        "{\"rules\":[]}",
                        "POLICY_VIOLATION",
                        createdAt
                )
        );

        assertThat(result.validationLogId()).isEqualTo(700L);
        assertThat(result.result()).isEqualTo("REJECTED");
        assertThat(result.assetStatus()).isEqualTo("REJECTED");
        verify(aiLogService).registerValidationLog(
                500L,
                33L,
                2,
                AiValidationResult.REJECTED,
                AiValidationReviewerType.AUTO,
                4L,
                "{\"rules\":[]}",
                "POLICY_VIOLATION",
                createdAt
        );
    }

    @Test
    void qaResponseAssetRegistrationIsRejectedInSystemGenerationFlow() {
        RegisterAiGeneratedAssetCommand command = new RegisterAiGeneratedAssetCommand(
                101L,
                "QA_RESPONSE",
                "QA_REQUEST",
                700L,
                "{\"answer\":\"validated answer\"}",
                null,
                OffsetDateTime.parse("2026-05-26T10:30:00+09:00")
        );

        assertThatThrownBy(() -> aiLogUseCaseService.registerAiGeneratedAsset(command))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT));
        verifyNoInteractions(aiLogService);
    }

    @Test
    void invalidEnumStringIsRejectedAsInvalidInput() {
        RegisterAiGeneratedAssetCommand command = new RegisterAiGeneratedAssetCommand(
                101L,
                "UNKNOWN",
                "QT_PASSAGE",
                35L,
                "{\"summary\":\"寃利??湲??댁꽕\"}",
                null,
                OffsetDateTime.parse("2026-05-26T10:30:00+09:00")
        );

        assertThatThrownBy(() -> aiLogUseCaseService.registerAiGeneratedAsset(command))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT));
        verifyNoInteractions(aiLogService);
    }

    @Test
    void nonPositiveValidationLogIdsAreRejectedAsInvalidInput() {
        RegisterAiValidationLogCommand command = new RegisterAiValidationLogCommand(
                0L,
                -1L,
                2,
                "PASSED",
                "AUTO",
                4L,
                "{\"rules\":[]}",
                null,
                OffsetDateTime.parse("2026-05-26T11:30:00+09:00")
        );

        assertThatThrownBy(() -> aiLogUseCaseService.registerAiValidationLog(command))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT));
        verifyNoInteractions(aiLogService);
    }

    @Test
    void nullCreatedAtIsRejectedAsInvalidInput() {
        RegisterAiValidationLogCommand command = new RegisterAiValidationLogCommand(
                500L,
                null,
                2,
                "PASSED",
                "AUTO",
                4L,
                "{\"rules\":[]}",
                null,
                null
        );

        assertThatThrownBy(() -> aiLogUseCaseService.registerAiValidationLog(command))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT));
        verifyNoInteractions(aiLogService);
    }
}
