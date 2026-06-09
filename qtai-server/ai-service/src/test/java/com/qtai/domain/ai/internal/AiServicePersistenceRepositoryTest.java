package com.qtai.domain.ai.internal;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Field;
import java.time.OffsetDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import com.qtai.ai.AiServiceApplication;

@SpringBootTest(
        classes = AiServiceApplication.class,
        properties = {
                "qtai.ai.persistence.enabled=true",
                "qtai.ai.persistence.url=jdbc:h2:mem:ai_service_persistence_repository;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
                "qtai.ai.persistence.username=sa",
                "qtai.ai.persistence.driver-class-name=org.h2.Driver",
                "qtai.ai.persistence.ddl-auto=create-drop"
        }
)
@ActiveProfiles("test")
class AiServicePersistenceRepositoryTest {

    private static final OffsetDateTime BASE_TIME = OffsetDateTime.parse("2026-06-09T09:00:00+09:00");

    @Autowired
    private AiGenerationJobRepository aiGenerationJobRepository;
    @Autowired
    private AiGeneratedAssetRepository aiGeneratedAssetRepository;
    @Autowired
    private AiValidationLogRepository aiValidationLogRepository;
    @Autowired
    private AiPromptVersionRepository aiPromptVersionRepository;
    @Autowired
    private AiValidationChecklistVersionRepository aiValidationChecklistVersionRepository;
    @Autowired
    private ValidationReferenceJobRepository validationReferenceJobRepository;
    @Autowired
    private AiBatchRunLogRepository aiBatchRunLogRepository;

    @Test
    void savesAndQueriesAiOwnedPersistenceModels() {
        AiPromptVersion promptVersion = aiPromptVersionRepository.saveAndFlush(promptVersion());
        AiGenerationJob job = aiGenerationJobRepository.saveAndFlush(AiGenerationJob.queue(
                AiGenerationJobType.EXPLANATION,
                AiTargetType.BIBLE_VERSE,
                101L,
                promptVersion.getId(),
                BASE_TIME
        ));
        AiGeneratedAsset asset = aiGeneratedAssetRepository.saveAndFlush(AiGeneratedAsset.create(
                job.getId(),
                AiGeneratedAssetType.EXPLANATION,
                AiTargetType.BIBLE_VERSE,
                101L,
                "{\"summary\":\"Allowed test summary\"}",
                "test-source",
                BASE_TIME.plusMinutes(1)
        ));
        AiValidationChecklistVersion checklistVersion = aiValidationChecklistVersionRepository.saveAndFlush(
                AiValidationChecklistVersion.create(
                        AiValidationChecklistType.EXPLANATION,
                        "2026.06.test",
                        "sha256:test-checklist",
                        1L,
                        BASE_TIME.plusMinutes(2)
                )
        );
        ValidationReferenceJob referenceJob = validationReferenceJobRepository.saveAndFlush(
                ValidationReferenceJob.create(
                        "Allowed Reference",
                        "allowed-reference.pdf",
                        "sha256:test-reference",
                        "s3://ai-service-test/reference.pdf",
                        "s3://ai-service-test/reference-index.json",
                        BASE_TIME.plusDays(7),
                        BASE_TIME.plusMinutes(3)
                )
        );
        AiValidationLog validationLog = aiValidationLogRepository.saveAndFlush(AiValidationLog.create(
                asset.getId(),
                referenceJob.getId(),
                1,
                AiValidationResult.PASSED,
                AiValidationReviewerType.AUTO,
                checklistVersion.getId(),
                "{\"checked\":true}",
                null,
                BASE_TIME.plusMinutes(4)
        ));
        AiBatchRunLog batchRunLog = aiBatchRunLogRepository.saveAndFlush(AiBatchRunLog.create(new AiBatchRunLogCommand(
                AiBatchName.AI_GENERATION_WORKER_POLL,
                AiBatchRunStatus.SUCCEEDED,
                1,
                0,
                1,
                null,
                null,
                BASE_TIME.plusMinutes(5),
                BASE_TIME.plusMinutes(6)
        )));

        assertThat(aiGenerationJobRepository.findQueuedJobIds(AiGenerationJobStatus.QUEUED, PageRequest.of(0, 10)))
                .containsExactly(job.getId());
        assertThat(aiGeneratedAssetRepository.findReadyExplanationBibleVerseTargetIds(java.util.List.of(101L)))
                .containsExactly(101L);
        assertThat(aiValidationLogRepository.findFirstByAiAssetIdAndLayerAndReviewerTypeOrderByCreatedAtDescIdDesc(
                asset.getId(),
                1,
                AiValidationReviewerType.AUTO
        )).map(AiValidationLog::getId).contains(validationLog.getId());
        assertThat(aiPromptVersionRepository.findFirstByPromptTypeAndStatusOrderByCreatedAtDescIdDesc(
                AiPromptType.EXPLANATION,
                AiPromptVersionStatus.ACTIVE
        )).map(AiPromptVersion::getId).contains(promptVersion.getId());
        assertThat(aiValidationChecklistVersionRepository.findByChecklistTypeAndStatus(
                AiValidationChecklistType.EXPLANATION,
                AiValidationChecklistStatus.DRAFT
        )).extracting(AiValidationChecklistVersion::getId).containsExactly(checklistVersion.getId());
        assertThat(validationReferenceJobRepository.findFirstByStatusOrderByCreatedAtDescIdDesc(
                ValidationReferenceJobStatus.ACTIVE
        )).map(ValidationReferenceJob::getId).contains(referenceJob.getId());
        assertThat(aiBatchRunLogRepository.findByStatusOrderByCreatedAtDescIdDesc(
                AiBatchRunStatus.SUCCEEDED,
                PageRequest.of(0, 10)
        )).extracting(AiBatchRunLog::getId).containsExactly(batchRunLog.getId());
        assertThat(batchRunLog.getCreatedAt()).isNotNull();
    }

    private static AiPromptVersion promptVersion() {
        AiPromptVersion promptVersion = new AiPromptVersion();
        setField(promptVersion, "promptType", AiPromptType.EXPLANATION);
        setField(promptVersion, "version", "2026.06.test");
        setField(promptVersion, "contentHash", "sha256:test-prompt");
        setField(promptVersion, "status", AiPromptVersionStatus.ACTIVE);
        setField(promptVersion, "createdAt", BASE_TIME.minusMinutes(1));
        return promptVersion;
    }

    private static void setField(Object target, String fieldName, Object value) {
        try {
            Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
