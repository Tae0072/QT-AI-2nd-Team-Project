package com.qtai.domain.ai.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.OffsetDateTime;

import org.junit.jupiter.api.Test;

import com.qtai.common.exception.BusinessException;

class AiServicePersistenceDomainPolicyTest {

    private static final OffsetDateTime BASE_TIME = OffsetDateTime.parse("2026-06-09T09:00:00+09:00");

    @Test
    void jsonStorageGuardRejectsRawProviderAndReferenceTextFields() {
        assertThatThrownBy(() -> AiJsonStorageGuard.rejectRawProviderOrReferenceText(
                "{\"providerRawResponse\":\"raw provider body\"}",
                "payloadJson"
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("payloadJson");

        assertThatThrownBy(() -> AiJsonStorageGuard.rejectRawProviderOrReferenceText(
                "{\"validation_reference_text\":\"raw reference body\"}",
                "checklistJson"
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("checklistJson");

        assertThatThrownBy(() -> AiJsonStorageGuard.rejectRawProviderOrReferenceText(
                "{\"commentaryOriginal\":\"raw commentary body\"}",
                "payloadJson"
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("payloadJson");

        assertThat(AiJsonStorageGuard.rejectRawProviderOrReferenceText(
                "{\"summary\":\"Allowed test summary\"}",
                "payloadJson"
        )).contains("Allowed test summary");
    }

    @Test
    void entitiesRejectInvalidStatusTransitions() {
        AiGenerationJob generationJob = AiGenerationJob.queue(
                AiGenerationJobType.EXPLANATION,
                AiTargetType.BIBLE_VERSE,
                101L,
                1L,
                BASE_TIME
        );
        assertThatThrownBy(() -> generationJob.markSucceeded(BASE_TIME.plusMinutes(1)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Invalid AI generation job status transition");

        AiGeneratedAsset asset = AiGeneratedAsset.create(
                1L,
                AiGeneratedAssetType.EXPLANATION,
                AiTargetType.BIBLE_VERSE,
                101L,
                "{\"summary\":\"Allowed test summary\"}",
                "test-source",
                BASE_TIME
        );
        asset.approve(BASE_TIME.plusMinutes(1));
        assertThatThrownBy(() -> asset.reject(BASE_TIME.plusMinutes(2)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Invalid AI generated asset status transition");

        AiValidationChecklistVersion checklistVersion = AiValidationChecklistVersion.create(
                AiValidationChecklistType.EXPLANATION,
                "2026.06.test",
                "sha256:test-checklist",
                1L,
                BASE_TIME
        );
        assertThatThrownBy(() -> checklistVersion.retire(BASE_TIME.plusMinutes(1)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Invalid AI validation checklist status transition");
    }

    @Test
    void generationJobCanFailBeforeRunningForQueueTimeValidationFailure() {
        AiGenerationJob generationJob = AiGenerationJob.queue(
                AiGenerationJobType.EXPLANATION,
                AiTargetType.BIBLE_VERSE,
                101L,
                1L,
                BASE_TIME
        );

        generationJob.markFailed("QUEUE_VALIDATION_FAILED", BASE_TIME.plusMinutes(1));

        assertThat(generationJob.getStatus()).isEqualTo(AiGenerationJobStatus.FAILED);
        assertThat(generationJob.getStartedAt()).isNull();
        assertThat(generationJob.getActiveUniqueKey()).isNull();
    }

    @Test
    void batchRunLogRedactsSensitiveErrorMessages() {
        AiBatchRunLog bearerError = AiBatchRunLog.create(new AiBatchRunLogCommand(
                AiBatchName.AI_GENERATION_WORKER_POLL,
                AiBatchRunStatus.FAILED,
                0,
                1,
                1,
                "IllegalStateException",
                "downstream failed with Bearer service-token-value",
                BASE_TIME,
                BASE_TIME.plusSeconds(1)
        ));
        AiBatchRunLog keyValueError = AiBatchRunLog.create(new AiBatchRunLogCommand(
                AiBatchName.AI_GENERATION_WORKER_POLL,
                AiBatchRunStatus.FAILED,
                0,
                1,
                1,
                "IllegalStateException",
                "provider password=hidden",
                BASE_TIME,
                BASE_TIME.plusSeconds(1)
        ));
        AiBatchRunLog safeError = AiBatchRunLog.create(new AiBatchRunLogCommand(
                AiBatchName.AI_GENERATION_WORKER_POLL,
                AiBatchRunStatus.FAILED,
                0,
                1,
                1,
                "IllegalStateException",
                "provider timeout",
                BASE_TIME,
                BASE_TIME.plusSeconds(1)
        ));

        assertThat(bearerError.getErrorMessage()).isEqualTo("REDACTED_SENSITIVE_ERROR_MESSAGE");
        assertThat(keyValueError.getErrorMessage()).isEqualTo("REDACTED_SENSITIVE_ERROR_MESSAGE");
        assertThat(safeError.getErrorMessage()).isEqualTo("provider timeout");
    }
}
