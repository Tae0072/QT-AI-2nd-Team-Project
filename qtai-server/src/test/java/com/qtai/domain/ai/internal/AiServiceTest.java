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

import com.fasterxml.jackson.databind.ObjectMapper;
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
import com.qtai.domain.audit.api.WriteAuditLogUseCase;
import com.qtai.domain.audit.api.dto.AuditLogWriteRequest;

class AiServiceTest {

    private static final OffsetDateTime CREATED_AT = OffsetDateTime.parse("2026-05-21T10:30:00+09:00");
    private static final List<AiGenerationJobStatus> ACTIVE_STATUSES = List.of(
            AiGenerationJobStatus.QUEUED,
            AiGenerationJobStatus.RUNNING
    );

    private AiGenerationJobRepository generationJobRepository;
    private AiGeneratedAssetRepository generatedAssetRepository;
    private AiPromptVersionRepository promptVersionRepository;
    private WriteAuditLogUseCase auditLogUseCase;
    private AiService aiService;

    @BeforeEach
    void setUp() {
        generationJobRepository = org.mockito.Mockito.mock(AiGenerationJobRepository.class);
        generatedAssetRepository = org.mockito.Mockito.mock(AiGeneratedAssetRepository.class);
        promptVersionRepository = org.mockito.Mockito.mock(AiPromptVersionRepository.class);
        auditLogUseCase = org.mockito.Mockito.mock(WriteAuditLogUseCase.class);
        aiService = new AiService(
                generationJobRepository,
                generatedAssetRepository,
                promptVersionRepository,
                auditLogUseCase,
                new ObjectMapper()
        );
    }

    @Test
    void aiServiceImplementsCreateAiGenerationJobUseCase() {
        assertThat(CreateAiGenerationJobUseCase.class).isAssignableFrom(AiService.class);
    }

    @Test
    void createAiGenerationJobCreatesQueuedJobWithPromptVersionId() {
        when(promptVersionRepository.findById(3L)).thenReturn(Optional.of(promptVersion(3L, AiPromptType.EXPLANATION)));
        when(generationJobRepository.existsByJobTypeAndTargetTypeAndTargetIdAndPromptVersionIdAndStatusIn(
                AiGenerationJobType.EXPLANATION,
                AiTargetType.QT_PASSAGE,
                35L,
                3L,
                ACTIVE_STATUSES
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
        assertThat(jobCaptor.getValue().getPromptVersionId()).isEqualTo(3L);
        assertThat(jobCaptor.getValue().getStatus()).isEqualTo(AiGenerationJobStatus.QUEUED);
        assertThat(jobCaptor.getValue().getCreatedAt()).isEqualTo(CREATED_AT);
    }

    @Test
    void createAiGenerationJobBlocksQueuedOrRunningDuplicateByPromptVersionId() {
        when(promptVersionRepository.findById(3L)).thenReturn(Optional.of(promptVersion(3L, AiPromptType.EXPLANATION)));
        when(generationJobRepository.existsByJobTypeAndTargetTypeAndTargetIdAndPromptVersionIdAndStatusIn(
                AiGenerationJobType.EXPLANATION,
                AiTargetType.QT_PASSAGE,
                35L,
                3L,
                ACTIVE_STATUSES
        )).thenReturn(true);

        assertThatThrownBy(() -> aiService.createAiGenerationJob(createJobCommand()))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_STATUS_TRANSITION));
        verify(generationJobRepository, never()).saveAndFlush(any(AiGenerationJob.class));
    }

    @Test
    void createAiGenerationJobMapsUniqueConstraintRaceToStatusTransitionError() {
        when(promptVersionRepository.findById(3L)).thenReturn(Optional.of(promptVersion(3L, AiPromptType.EXPLANATION)));
        when(generationJobRepository.existsByJobTypeAndTargetTypeAndTargetIdAndPromptVersionIdAndStatusIn(
                AiGenerationJobType.EXPLANATION,
                AiTargetType.QT_PASSAGE,
                35L,
                3L,
                ACTIVE_STATUSES
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
        when(promptVersionRepository.findById(3L)).thenReturn(Optional.of(promptVersion(3L, AiPromptType.EXPLANATION)));
        when(generationJobRepository.existsByJobTypeAndTargetTypeAndTargetIdAndPromptVersionIdAndStatusIn(
                AiGenerationJobType.EXPLANATION,
                AiTargetType.QT_PASSAGE,
                35L,
                3L,
                ACTIVE_STATUSES
        )).thenReturn(false);
        when(generationJobRepository.saveAndFlush(any(AiGenerationJob.class)))
                .thenThrow(new DataIntegrityViolationException("not-null violation on created_at"));

        assertThatThrownBy(() -> aiService.createAiGenerationJob(createJobCommand()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void missingPromptVersionIsBlocked() {
        when(promptVersionRepository.findById(3L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> aiService.createAiGenerationJob(createJobCommand()))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT));
        verify(generationJobRepository, never()).saveAndFlush(any(AiGenerationJob.class));
    }

    @Test
    void retiredPromptVersionIsBlocked() {
        when(promptVersionRepository.findById(3L))
                .thenReturn(Optional.of(promptVersion(3L, AiPromptType.EXPLANATION, AiPromptVersionStatus.RETIRED)));

        assertThatThrownBy(() -> aiService.createAiGenerationJob(createJobCommand()))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT));
        verify(generationJobRepository, never()).saveAndFlush(any(AiGenerationJob.class));
    }

    @Test
    void promptTypeMismatchIsBlocked() {
        when(promptVersionRepository.findById(3L)).thenReturn(Optional.of(promptVersion(3L, AiPromptType.SIMULATOR)));

        assertThatThrownBy(() -> aiService.createAiGenerationJob(createJobCommand()))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT));
        verify(generationJobRepository, never()).saveAndFlush(any(AiGenerationJob.class));
    }

    @Test
    void summaryAndGlossaryJobTypesAreNotSupportedByPromptVersionMapping() {
        when(promptVersionRepository.findById(3L)).thenReturn(Optional.of(promptVersion(3L, AiPromptType.EXPLANATION)));

        assertThatThrownBy(() -> aiService.createAiGenerationJob(createJobCommand("SUMMARY", "QT_PASSAGE")))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT));

        assertThatThrownBy(() -> aiService.createAiGenerationJob(createJobCommand("GLOSSARY", "QT_PASSAGE")))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT));
        verify(generationJobRepository, never()).saveAndFlush(any(AiGenerationJob.class));
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
        assertInvalidCreateCommand(new CreateAiGenerationJobCommand(
                "EXPLANATION",
                "QT_PASSAGE",
                0L,
                3L,
                "SYSTEM_BATCH",
                CREATED_AT
        ));
        assertInvalidCreateCommand(new CreateAiGenerationJobCommand(
                "EXPLANATION",
                "QT_PASSAGE",
                35L,
                0L,
                "SYSTEM_BATCH",
                CREATED_AT
        ));
        assertInvalidCreateCommand(new CreateAiGenerationJobCommand(
                "EXPLANATION",
                "QT_PASSAGE",
                35L,
                3L,
                " ",
                CREATED_AT
        ));
        assertInvalidCreateCommand(new CreateAiGenerationJobCommand(
                "EXPLANATION",
                "QT_PASSAGE",
                35L,
                3L,
                "SYSTEM_BATCH",
                null
        ));
        verify(generationJobRepository, never()).saveAndFlush(any(AiGenerationJob.class));
    }

    @Test
    void rejectedAssetCreatesQueuedRegenerationJobWithPromptVersionId() {
        AiGeneratedAsset asset = assetWithStatus(AiGeneratedAssetStatus.REJECTED);
        when(generatedAssetRepository.findById(500L)).thenReturn(Optional.of(asset));
        when(promptVersionRepository.findById(3L)).thenReturn(Optional.of(promptVersion(3L, AiPromptType.EXPLANATION)));
        when(generationJobRepository.existsByJobTypeAndTargetTypeAndTargetIdAndPromptVersionIdAndStatusIn(
                AiGenerationJobType.EXPLANATION,
                AiTargetType.BIBLE_VERSE,
                1001L,
                3L,
                ACTIVE_STATUSES
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
        assertThat(jobCaptor.getValue().getPromptVersionId()).isEqualTo(3L);
    }

    @Test
    void rejectedAssetRegenerationWritesSafeAuditLog() {
        AiGeneratedAsset asset = assetWithStatus(AiGeneratedAssetStatus.REJECTED);
        setId(asset, 500L);
        when(generatedAssetRepository.findById(500L)).thenReturn(Optional.of(asset));
        when(promptVersionRepository.findById(3L)).thenReturn(Optional.of(promptVersion(3L, AiPromptType.EXPLANATION)));
        when(generationJobRepository.existsByJobTypeAndTargetTypeAndTargetIdAndPromptVersionIdAndStatusIn(
                AiGenerationJobType.EXPLANATION,
                AiTargetType.BIBLE_VERSE,
                1001L,
                3L,
                ACTIVE_STATUSES
        )).thenReturn(false);
        when(generationJobRepository.saveAndFlush(any(AiGenerationJob.class))).thenAnswer(invocation -> {
            AiGenerationJob job = invocation.getArgument(0);
            setId(job, 101L);
            return job;
        });

        aiService.regenerateAiAsset(adminCommand("REVIEWER"));

        ArgumentCaptor<AuditLogWriteRequest> auditCaptor = ArgumentCaptor.forClass(AuditLogWriteRequest.class);
        verify(auditLogUseCase).write(auditCaptor.capture());
        AuditLogWriteRequest audit = auditCaptor.getValue();
        assertThat(audit.actorType()).isEqualTo("ADMIN");
        assertThat(audit.actorId()).isEqualTo(7L);
        assertThat(audit.actorLabel()).isEqualTo("ADMIN:7");
        assertThat(audit.actionType()).isEqualTo("AI_REGENERATE_REQUEST");
        assertThat(audit.targetType()).isEqualTo("AI_GENERATED_ASSET");
        assertThat(audit.targetId()).isEqualTo(500L);
        assertThat(audit.beforeJson())
                .contains("\"id\":500", "\"assetType\":\"EXPLANATION\"", "\"status\":\"REJECTED\"",
                        "\"targetType\":\"BIBLE_VERSE\"", "\"targetId\":1001");
        assertThat(audit.afterJson())
                .contains("\"id\":101", "\"status\":\"QUEUED\"", "\"jobType\":\"EXPLANATION\"",
                        "\"targetType\":\"BIBLE_VERSE\"", "\"targetId\":1001", "\"promptVersionId\":3",
                        "\"requestedAt\":\"2026-05-21T10:30:00+09:00\"");
        assertThat(audit.beforeJson() + audit.afterJson())
                .doesNotContain(
                        "regenerate reason",
                        "payloadJson",
                        "contentHash",
                        "rawResponse",
                        "providerRawResponse",
                        "promptText",
                        "validationReferenceText",
                        "secret",
                        "token",
                        "password",
                        "privateKey"
                );
    }

    @Test
    void hiddenAssetCanBeRegeneratedBySuperAdmin() {
        AiGeneratedAsset asset = assetWithStatus(AiGeneratedAssetStatus.HIDDEN);
        when(generatedAssetRepository.findById(500L)).thenReturn(Optional.of(asset));
        when(promptVersionRepository.findById(3L)).thenReturn(Optional.of(promptVersion(3L, AiPromptType.EXPLANATION)));
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
        verify(auditLogUseCase, never()).write(any(AuditLogWriteRequest.class));
    }

    @Test
    void approvedAssetCannotBeRegeneratedInThisWorkflow() {
        AiGeneratedAsset asset = assetWithStatus(AiGeneratedAssetStatus.APPROVED);
        when(generatedAssetRepository.findById(500L)).thenReturn(Optional.of(asset));

        assertThatThrownBy(() -> aiService.regenerateAiAsset(adminCommand("REVIEWER")))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_STATUS_TRANSITION));
        verify(generationJobRepository, never()).saveAndFlush(any(AiGenerationJob.class));
        verify(auditLogUseCase, never()).write(any(AuditLogWriteRequest.class));
    }

    @Test
    void existingQueuedOrRunningJobBlocksDuplicateRegeneration() {
        AiGeneratedAsset asset = assetWithStatus(AiGeneratedAssetStatus.REJECTED);
        when(generatedAssetRepository.findById(500L)).thenReturn(Optional.of(asset));
        when(promptVersionRepository.findById(3L)).thenReturn(Optional.of(promptVersion(3L, AiPromptType.EXPLANATION)));
        when(generationJobRepository.existsByJobTypeAndTargetTypeAndTargetIdAndPromptVersionIdAndStatusIn(
                AiGenerationJobType.EXPLANATION,
                AiTargetType.BIBLE_VERSE,
                1001L,
                3L,
                ACTIVE_STATUSES
        )).thenReturn(true);

        assertThatThrownBy(() -> aiService.regenerateAiAsset(adminCommand("REVIEWER")))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_STATUS_TRANSITION));
        verify(generationJobRepository, never()).saveAndFlush(any(AiGenerationJob.class));
        verify(auditLogUseCase, never()).write(any(AuditLogWriteRequest.class));
    }

    @Test
    void regenerateAiAssetMapsUniqueConstraintRaceToStatusTransitionError() {
        AiGeneratedAsset asset = assetWithStatus(AiGeneratedAssetStatus.REJECTED);
        when(generatedAssetRepository.findById(500L)).thenReturn(Optional.of(asset));
        when(promptVersionRepository.findById(3L)).thenReturn(Optional.of(promptVersion(3L, AiPromptType.EXPLANATION)));
        when(generationJobRepository.existsByJobTypeAndTargetTypeAndTargetIdAndPromptVersionIdAndStatusIn(
                AiGenerationJobType.EXPLANATION,
                AiTargetType.BIBLE_VERSE,
                1001L,
                3L,
                ACTIVE_STATUSES
        )).thenReturn(false);
        when(generationJobRepository.saveAndFlush(any(AiGenerationJob.class)))
                .thenThrow(new DataIntegrityViolationException(
                        "Duplicate entry for key 'uk_ai_generation_jobs_active_target_prompt'"));

        assertThatThrownBy(() -> aiService.regenerateAiAsset(adminCommand("REVIEWER")))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_STATUS_TRANSITION));
        verify(auditLogUseCase, never()).write(any(AuditLogWriteRequest.class));
    }

    @Test
    void regenerateAiAssetBlocksPromptVersionTypeMismatch() {
        AiGeneratedAsset asset = assetWithStatus(AiGeneratedAssetStatus.REJECTED);
        when(generatedAssetRepository.findById(500L)).thenReturn(Optional.of(asset));
        when(promptVersionRepository.findById(3L)).thenReturn(Optional.of(promptVersion(3L, AiPromptType.SIMULATOR)));

        assertThatThrownBy(() -> aiService.regenerateAiAsset(adminCommand("REVIEWER")))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT));
        verify(generationJobRepository, never()).saveAndFlush(any(AiGenerationJob.class));
    }

    @Test
    void memberRoleAdminAndReviewerOrSuperAdminRoleAreRequired() {
        assertThatThrownBy(() -> aiService.regenerateAiAsset(command("USER", "REVIEWER")))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN));

        assertThatThrownBy(() -> aiService.regenerateAiAsset(command("ADMIN", "OPERATOR")))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN));
        verify(auditLogUseCase, never()).write(any(AuditLogWriteRequest.class));
    }

    @Test
    void invalidRegenerateAiAssetCommandFieldsAreBlockedBeforeAuthorization() {
        assertInvalidRegenerateCommand(new RegenerateAiAssetCommand(
                7L,
                500L,
                " ",
                "REVIEWER",
                "regenerate reason",
                3L,
                CREATED_AT
        ));
        assertInvalidRegenerateCommand(new RegenerateAiAssetCommand(
                7L,
                500L,
                "ADMIN",
                "",
                "regenerate reason",
                3L,
                CREATED_AT
        ));
        assertInvalidRegenerateCommand(new RegenerateAiAssetCommand(
                7L,
                500L,
                "ADMIN",
                "REVIEWER",
                "regenerate reason",
                0L,
                CREATED_AT
        ));
        verify(generatedAssetRepository, never()).findById(any());
        verify(generationJobRepository, never()).saveAndFlush(any(AiGenerationJob.class));
        verify(auditLogUseCase, never()).write(any(AuditLogWriteRequest.class));
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
                3L,
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
                "regenerate reason",
                3L,
                CREATED_AT
        );
    }

    private static AiPromptVersion promptVersion(Long id, AiPromptType promptType) {
        return promptVersion(id, promptType, AiPromptVersionStatus.ACTIVE);
    }

    private static AiPromptVersion promptVersion(
            Long id,
            AiPromptType promptType,
            AiPromptVersionStatus status
    ) {
        return AiPromptVersion.of(
                id,
                promptType,
                "2026.05.1",
                "hash-" + id,
                status,
                CREATED_AT.minusDays(1)
        );
    }

    private void assertInvalidCreateCommand(CreateAiGenerationJobCommand command) {
        assertThatThrownBy(() -> aiService.createAiGenerationJob(command))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT));
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
                "{\"summary\":\"validated\"}",
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
