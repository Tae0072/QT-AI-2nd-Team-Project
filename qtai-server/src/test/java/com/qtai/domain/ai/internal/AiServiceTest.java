package com.qtai.domain.ai.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataIntegrityViolationException;

import com.qtai.common.exception.BusinessException;
import com.qtai.common.exception.ErrorCode;
import com.qtai.domain.ai.api.CreateAiGenerationJobUseCase;
import com.qtai.domain.ai.api.dto.CreateAiGenerationJobCommand;
import com.qtai.domain.ai.api.dto.CreateAiGenerationJobResult;
import com.qtai.domain.ai.api.dto.RegenerateAiAssetCommand;
import com.qtai.domain.ai.api.dto.RegenerateAiAssetResult;

class AiServiceTest {

    private static final OffsetDateTime CREATED_AT = OffsetDateTime.parse("2026-05-21T10:30:00+09:00");

    private AiGenerationJobRepository generationJobRepository;
    private AiGeneratedAssetRepository generatedAssetRepository;
    private AiService aiService;

    @BeforeEach
    void setUp() {
        generationJobRepository = org.mockito.Mockito.mock(AiGenerationJobRepository.class);
        generatedAssetRepository = org.mockito.Mockito.mock(AiGeneratedAssetRepository.class);
        aiService = new AiService(generationJobRepository, generatedAssetRepository);
    }

    @Test
    void aiServiceImplementsCreateAiGenerationJobUseCase() {
        assertThat(CreateAiGenerationJobUseCase.class).isAssignableFrom(AiService.class);
    }

    @Test
    void createAiGenerationJobCreatesQueuedJob() {
        when(generationJobRepository.existsByJobTypeAndTargetTypeAndTargetIdAndPromptVersionAndStatusIn(
                AiGenerationJobType.EXPLANATION,
                AiTargetType.QT_PASSAGE,
                35L,
                "2026.05.1",
                List.of(AiGenerationJobStatus.QUEUED, AiGenerationJobStatus.RUNNING)
        )).thenReturn(false);
        when(generationJobRepository.saveAndFlush(any(AiGenerationJob.class))).thenAnswer(invocation -> {
            AiGenerationJob job = invocation.getArgument(0);
            setId(job, 201L);
            return job;
        });

        CreateAiGenerationJobResult result = aiService.createAiGenerationJob(createJobCommand());

        assertThat(result.generationJobId()).isEqualTo(201L);
        assertThat(result.status()).isEqualTo("QUEUED");

        ArgumentCaptor<AiGenerationJob> jobCaptor = ArgumentCaptor.forClass(AiGenerationJob.class);
        verify(generationJobRepository).saveAndFlush(jobCaptor.capture());
        assertThat(jobCaptor.getValue().getJobType()).isEqualTo(AiGenerationJobType.EXPLANATION);
        assertThat(jobCaptor.getValue().getTargetType()).isEqualTo(AiTargetType.QT_PASSAGE);
        assertThat(jobCaptor.getValue().getTargetId()).isEqualTo(35L);
        assertThat(jobCaptor.getValue().getPromptVersion()).isEqualTo("2026.05.1");
        assertThat(jobCaptor.getValue().getStatus()).isEqualTo(AiGenerationJobStatus.QUEUED);
        assertThat(jobCaptor.getValue().getCreatedAt()).isEqualTo(CREATED_AT);
    }

    @Test
    void createAiGenerationJobBlocksQueuedOrRunningDuplicate() {
        when(generationJobRepository.existsByJobTypeAndTargetTypeAndTargetIdAndPromptVersionAndStatusIn(
                AiGenerationJobType.EXPLANATION,
                AiTargetType.QT_PASSAGE,
                35L,
                "2026.05.1",
                List.of(AiGenerationJobStatus.QUEUED, AiGenerationJobStatus.RUNNING)
        )).thenReturn(true);

        assertThatThrownBy(() -> aiService.createAiGenerationJob(createJobCommand()))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_STATUS_TRANSITION));
        verify(generationJobRepository, never()).saveAndFlush(any(AiGenerationJob.class));
    }

    @Test
    void createAiGenerationJobMapsUniqueConstraintRaceToStatusTransitionError() {
        when(generationJobRepository.existsByJobTypeAndTargetTypeAndTargetIdAndPromptVersionAndStatusIn(
                AiGenerationJobType.EXPLANATION,
                AiTargetType.QT_PASSAGE,
                35L,
                "2026.05.1",
                List.of(AiGenerationJobStatus.QUEUED, AiGenerationJobStatus.RUNNING)
        )).thenReturn(false);
        when(generationJobRepository.saveAndFlush(any(AiGenerationJob.class)))
                .thenThrow(new DataIntegrityViolationException(
                        "Duplicate entry for key 'uk_ai_generation_jobs_active_target_prompt'"));

        assertThatThrownBy(() -> aiService.createAiGenerationJob(createJobCommand()))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_STATUS_TRANSITION));
    }

    @Test
    void createAiGenerationJobDoesNotMapUnrelatedDataIntegrityViolation() {
        when(generationJobRepository.existsByJobTypeAndTargetTypeAndTargetIdAndPromptVersionAndStatusIn(
                AiGenerationJobType.EXPLANATION,
                AiTargetType.QT_PASSAGE,
                35L,
                "2026.05.1",
                List.of(AiGenerationJobStatus.QUEUED, AiGenerationJobStatus.RUNNING)
        )).thenReturn(false);
        when(generationJobRepository.saveAndFlush(any(AiGenerationJob.class)))
                .thenThrow(new DataIntegrityViolationException("not-null violation on created_at"));

        assertThatThrownBy(() -> aiService.createAiGenerationJob(createJobCommand()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void nullCreateAiGenerationJobCommandIsBlocked() {
        assertThatThrownBy(() -> aiService.createAiGenerationJob(null))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT));
        verify(generationJobRepository, never()).saveAndFlush(any(AiGenerationJob.class));
    }

    @Test
    void invalidCreateAiGenerationJobEnumValuesAreBlocked() {
        assertThatThrownBy(() -> aiService.createAiGenerationJob(createJobCommand("DAILY_QT_EXPLANATION", "QT_PASSAGE")))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT));

        assertThatThrownBy(() -> aiService.createAiGenerationJob(createJobCommand("EXPLANATION", "UNKNOWN_TARGET")))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT));
        verify(generationJobRepository, never()).saveAndFlush(any(AiGenerationJob.class));
    }

    @Test
    void invalidCreateAiGenerationJobRequiredFieldsAreBlocked() {
        assertThatThrownBy(() -> aiService.createAiGenerationJob(new CreateAiGenerationJobCommand(
                "EXPLANATION",
                "QT_PASSAGE",
                0L,
                "2026.05.1",
                "SYSTEM_BATCH",
                CREATED_AT
        ))).isInstanceOfSatisfying(BusinessException.class, exception ->
                assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT));

        assertThatThrownBy(() -> aiService.createAiGenerationJob(new CreateAiGenerationJobCommand(
                "EXPLANATION",
                "QT_PASSAGE",
                35L,
                " ",
                "SYSTEM_BATCH",
                CREATED_AT
        ))).isInstanceOfSatisfying(BusinessException.class, exception ->
                assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT));

        assertThatThrownBy(() -> aiService.createAiGenerationJob(new CreateAiGenerationJobCommand(
                "EXPLANATION",
                "QT_PASSAGE",
                35L,
                "2026.05.1",
                " ",
                CREATED_AT
        ))).isInstanceOfSatisfying(BusinessException.class, exception ->
                assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT));

        assertThatThrownBy(() -> aiService.createAiGenerationJob(new CreateAiGenerationJobCommand(
                "EXPLANATION",
                "QT_PASSAGE",
                35L,
                "2026.05.1",
                "SYSTEM_BATCH",
                null
        ))).isInstanceOfSatisfying(BusinessException.class, exception ->
                assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT));
        verify(generationJobRepository, never()).saveAndFlush(any(AiGenerationJob.class));
    }

    @Test
    void rejectedAssetCreatesQueuedRegenerationJob() {
        AiGeneratedAsset asset = assetWithStatus(AiGeneratedAssetStatus.REJECTED);
        when(generatedAssetRepository.findById(500L)).thenReturn(Optional.of(asset));
        when(generationJobRepository.existsByJobTypeAndTargetTypeAndTargetIdAndPromptVersionAndStatusIn(
                AiGenerationJobType.EXPLANATION,
                AiTargetType.BIBLE_VERSE,
                1001L,
                "2026.05.1",
                List.of(AiGenerationJobStatus.QUEUED, AiGenerationJobStatus.RUNNING)
        )).thenReturn(false);
        when(generationJobRepository.saveAndFlush(any(AiGenerationJob.class))).thenAnswer(invocation -> {
            AiGenerationJob job = invocation.getArgument(0);
            setId(job, 101L);
            return job;
        });

        RegenerateAiAssetResult result = aiService.regenerateAiAsset(adminCommand("REVIEWER"));

        assertThat(result.generationJobId()).isEqualTo(101L);
        assertThat(result.status()).isEqualTo("QUEUED");
        assertThat(result.createdAt()).isEqualTo(CREATED_AT);

        ArgumentCaptor<AiGenerationJob> jobCaptor = ArgumentCaptor.forClass(AiGenerationJob.class);
        verify(generationJobRepository).saveAndFlush(jobCaptor.capture());
        assertThat(jobCaptor.getValue().getStatus()).isEqualTo(AiGenerationJobStatus.QUEUED);
        assertThat(jobCaptor.getValue().getJobType()).isEqualTo(AiGenerationJobType.EXPLANATION);
        assertThat(jobCaptor.getValue().getTargetType()).isEqualTo(AiTargetType.BIBLE_VERSE);
        assertThat(jobCaptor.getValue().getTargetId()).isEqualTo(1001L);
        assertThat(jobCaptor.getValue().getPromptVersion()).isEqualTo("2026.05.1");
    }

    @Test
    void hiddenAssetCanBeRegeneratedBySuperAdmin() {
        AiGeneratedAsset asset = assetWithStatus(AiGeneratedAssetStatus.HIDDEN);
        when(generatedAssetRepository.findById(500L)).thenReturn(Optional.of(asset));
        when(generationJobRepository.saveAndFlush(any(AiGenerationJob.class))).thenAnswer(invocation -> {
            AiGenerationJob job = invocation.getArgument(0);
            setId(job, 102L);
            return job;
        });

        RegenerateAiAssetResult result = aiService.regenerateAiAsset(adminCommand("SUPER_ADMIN"));

        assertThat(result.generationJobId()).isEqualTo(102L);
        assertThat(result.status()).isEqualTo("QUEUED");
    }

    @Test
    void validatingAssetCannotBeRegenerated() {
        AiGeneratedAsset asset = assetWithStatus(AiGeneratedAssetStatus.VALIDATING);
        when(generatedAssetRepository.findById(500L)).thenReturn(Optional.of(asset));

        assertThatThrownBy(() -> aiService.regenerateAiAsset(adminCommand("REVIEWER")))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_STATUS_TRANSITION));
        verify(generationJobRepository, never()).saveAndFlush(any(AiGenerationJob.class));
    }

    @Test
    void approvedAssetCannotBeRegeneratedInThisWorkflow() {
        AiGeneratedAsset asset = assetWithStatus(AiGeneratedAssetStatus.APPROVED);
        when(generatedAssetRepository.findById(500L)).thenReturn(Optional.of(asset));

        assertThatThrownBy(() -> aiService.regenerateAiAsset(adminCommand("REVIEWER")))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_STATUS_TRANSITION));
        verify(generationJobRepository, never()).saveAndFlush(any(AiGenerationJob.class));
    }

    @Test
    void existingQueuedOrRunningJobBlocksDuplicateRegeneration() {
        AiGeneratedAsset asset = assetWithStatus(AiGeneratedAssetStatus.REJECTED);
        when(generatedAssetRepository.findById(500L)).thenReturn(Optional.of(asset));
        when(generationJobRepository.existsByJobTypeAndTargetTypeAndTargetIdAndPromptVersionAndStatusIn(
                AiGenerationJobType.EXPLANATION,
                AiTargetType.BIBLE_VERSE,
                1001L,
                "2026.05.1",
                List.of(AiGenerationJobStatus.QUEUED, AiGenerationJobStatus.RUNNING)
        )).thenReturn(true);

        assertThatThrownBy(() -> aiService.regenerateAiAsset(adminCommand("REVIEWER")))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_STATUS_TRANSITION));
        verify(generationJobRepository, never()).saveAndFlush(any(AiGenerationJob.class));
    }

    @Test
    void regenerateAiAssetMapsUniqueConstraintRaceToStatusTransitionError() {
        AiGeneratedAsset asset = assetWithStatus(AiGeneratedAssetStatus.REJECTED);
        when(generatedAssetRepository.findById(500L)).thenReturn(Optional.of(asset));
        when(generationJobRepository.existsByJobTypeAndTargetTypeAndTargetIdAndPromptVersionAndStatusIn(
                AiGenerationJobType.EXPLANATION,
                AiTargetType.BIBLE_VERSE,
                1001L,
                "2026.05.1",
                List.of(AiGenerationJobStatus.QUEUED, AiGenerationJobStatus.RUNNING)
        )).thenReturn(false);
        when(generationJobRepository.saveAndFlush(any(AiGenerationJob.class)))
                .thenThrow(new DataIntegrityViolationException(
                        "Duplicate entry for key 'uk_ai_generation_jobs_active_target_prompt'"));

        assertThatThrownBy(() -> aiService.regenerateAiAsset(adminCommand("REVIEWER")))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_STATUS_TRANSITION));
    }

    @Test
    void memberRoleAdminAndReviewerOrSuperAdminRoleAreRequired() {
        assertThatThrownBy(() -> aiService.regenerateAiAsset(command("USER", "REVIEWER")))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN));

        assertThatThrownBy(() -> aiService.regenerateAiAsset(command("ADMIN", "OPERATOR")))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN));
    }

    @Test
    void invalidRegenerateAiAssetCommandFieldsAreBlockedBeforeAuthorization() {
        assertInvalidRegenerateCommand(new RegenerateAiAssetCommand(
                7L,
                500L,
                " ",
                "REVIEWER",
                "출처 표기가 부족합니다.",
                3L,
                CREATED_AT
        ));
        assertInvalidRegenerateCommand(new RegenerateAiAssetCommand(
                7L,
                500L,
                "ADMIN",
                "",
                "출처 표기가 부족합니다.",
                3L,
                CREATED_AT
        ));
        assertInvalidRegenerateCommand(new RegenerateAiAssetCommand(
                7L,
                500L,
                "ADMIN",
                "REVIEWER",
                "출처 표기가 부족합니다.",
                0L,
                CREATED_AT
        ));
        verify(generatedAssetRepository, never()).findById(any());
        verify(generationJobRepository, never()).saveAndFlush(any(AiGenerationJob.class));
    }

    private static RegenerateAiAssetCommand adminCommand(String adminRole) {
        return command("ADMIN", adminRole);
    }

    private static CreateAiGenerationJobCommand createJobCommand() {
        return createJobCommand("EXPLANATION", "QT_PASSAGE");
    }

    private static CreateAiGenerationJobCommand createJobCommand(String jobType, String targetType) {
        return new CreateAiGenerationJobCommand(
                jobType,
                targetType,
                35L,
                "2026.05.1",
                "SYSTEM_BATCH",
                CREATED_AT
        );
    }

    private static RegenerateAiAssetCommand command(String memberRole, String adminRole) {
        return new RegenerateAiAssetCommand(
                7L,
                500L,
                memberRole,
                adminRole,
                "출처 표기가 부족합니다.",
                3L,
                CREATED_AT
        );
    }

    private void assertInvalidRegenerateCommand(RegenerateAiAssetCommand command) {
        assertThatThrownBy(() -> aiService.regenerateAiAsset(command))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT));
    }

    private static AiGeneratedAsset assetWithStatus(AiGeneratedAssetStatus status) {
        AiGeneratedAsset asset = AiGeneratedAsset.create(
                1L,
                AiGeneratedAssetType.EXPLANATION,
                AiTargetType.BIBLE_VERSE,
                1001L,
                "2026.05.1",
                "{\"summary\":\"검증 대상\"}",
                "QT-AI verified content",
                CREATED_AT.minusHours(1)
        );
        if (status == AiGeneratedAssetStatus.APPROVED) {
            asset.approve(CREATED_AT.minusMinutes(20));
        } else if (status == AiGeneratedAssetStatus.REJECTED) {
            asset.reject(CREATED_AT.minusMinutes(20));
        } else if (status == AiGeneratedAssetStatus.HIDDEN) {
            asset.hide(CREATED_AT.minusMinutes(20));
        }
        return asset;
    }

    private static void setId(Object target, Long id) {
        try {
            Field idField = target.getClass().getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(target, id);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
