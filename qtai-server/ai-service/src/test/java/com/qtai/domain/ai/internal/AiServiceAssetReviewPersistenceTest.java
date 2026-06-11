package com.qtai.domain.ai.internal;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.qtai.ai.AiServiceApplication;
import com.qtai.domain.ai.api.admin.asset.RegenerateAiAssetUseCase;
import com.qtai.domain.ai.api.admin.asset.ReviewAiAssetUseCase;
import com.qtai.domain.ai.api.admin.asset.dto.RegenerateAiAssetCommand;
import com.qtai.domain.ai.api.admin.asset.dto.ReviewAiAssetCommand;
import com.qtai.domain.ai.client.audit.AuditLogClientMock;
import com.qtai.domain.ai.client.study.StudyPublishClientMock;

@SpringBootTest(
        classes = AiServiceApplication.class,
        properties = {
                "qtai.ai.inbound.enabled=true",
                "qtai.ai.persistence.enabled=true",
                "qtai.ai.persistence.url=jdbc:h2:mem:ai_service_asset_review;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
                "qtai.ai.persistence.username=sa",
                "qtai.ai.persistence.driver-class-name=org.h2.Driver",
                "qtai.ai.persistence.ddl-auto=create-drop",
                "qtai.ai.persistence.flyway-enabled=false"
        }
)
@ActiveProfiles("test")
class AiServiceAssetReviewPersistenceTest {

    private static final OffsetDateTime BASE_TIME = OffsetDateTime.parse("2026-06-09T09:00:00+09:00");

    @Autowired
    private ReviewAiAssetUseCase reviewAiAssetUseCase;
    @Autowired
    private RegenerateAiAssetUseCase regenerateAiAssetUseCase;
    @Autowired
    private AiPromptVersionRepository promptVersionRepository;
    @Autowired
    private AiGenerationJobRepository generationJobRepository;
    @Autowired
    private AiGeneratedAssetRepository generatedAssetRepository;
    @Autowired
    private AiValidationChecklistVersionRepository checklistVersionRepository;
    @Autowired
    private AiValidationLogRepository validationLogRepository;
    @Autowired
    private AiEventOutboxRepository eventOutboxRepository;
    @Autowired
    private StudyPublishClientMock studyPublishClientMock;
    @Autowired
    private AuditLogClientMock auditLogClientMock;
    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        validationLogRepository.deleteAll();
        generatedAssetRepository.deleteAll();
        eventOutboxRepository.deleteAll();
        generationJobRepository.deleteAll();
        checklistVersionRepository.deleteAll();
        promptVersionRepository.deleteAll();
    }

    @Test
    void approveHideAndRegenerateUseOwnedDbAndOutboundClients() throws Exception {
        AiPromptVersion promptVersion = promptVersionRepository.saveAndFlush(promptVersion(3L, "2026.06.review"));
        AiValidationChecklistVersion checklistVersion = checklistVersionRepository.saveAndFlush(checklistVersion());
        AiGeneratedAsset asset = saveExplanationVerseAsset(promptVersion, 501L);
        savePassedValidation(asset, checklistVersion, 1, AiValidationReviewerType.AUTO);
        savePassedValidation(asset, checklistVersion, 2, AiValidationReviewerType.ADVISOR);

        var approved = reviewAiAssetUseCase.reviewAiAsset(new ReviewAiAssetCommand(
                107L,
                asset.getId(),
                "ADMIN",
                "REVIEWER",
                "APPROVE",
                "approved",
                true,
                BASE_TIME.plusMinutes(10)
        ));
        assertThat(approved.status()).isEqualTo("APPROVED");
        assertThat(studyPublishClientMock.publishedCommands())
                .anySatisfy(command -> {
                    assertThat(command.bibleVerseId()).isEqualTo(501L);
                    assertThat(command.aiAssetId()).isEqualTo(asset.getId());
                });

        var hidden = reviewAiAssetUseCase.reviewAiAsset(new ReviewAiAssetCommand(
                107L,
                asset.getId(),
                "ADMIN",
                "REVIEWER",
                "HIDE",
                "hide after approval",
                false,
                BASE_TIME.plusMinutes(11)
        ));
        assertThat(hidden.status()).isEqualTo("HIDDEN");
        assertThat(studyPublishClientMock.hiddenCommands())
                .anySatisfy(command -> assertThat(command.aiAssetId()).isEqualTo(asset.getId()));

        var regenerated = regenerateAiAssetUseCase.regenerateAiAsset(new RegenerateAiAssetCommand(
                107L,
                asset.getId(),
                "ADMIN",
                "REVIEWER",
                "retry hidden asset",
                promptVersion.getId(),
                BASE_TIME.plusMinutes(12)
        ));
        assertThat(regenerated.status()).isEqualTo("QUEUED");
        assertThat(generationJobRepository.findById(regenerated.generationJobId()))
                .map(AiGenerationJob::getTargetId)
                .contains(501L);
        assertAdminRegenerateRequestedOutbox(regenerated.generationJobId(), promptVersion.getId());
        assertThat(auditLogClientMock.writtenCommands())
                .anySatisfy(command -> assertThat(command.actionType()).isEqualTo("AI_REGENERATE_REQUEST"));
    }

    @Test
    void rejectUpdatesAssetStatusAndWritesAudit() {
        AiPromptVersion promptVersion = promptVersionRepository.saveAndFlush(promptVersion(4L, "2026.06.reject"));
        AiGeneratedAsset asset = saveExplanationVerseAsset(promptVersion, 777L);

        var rejected = reviewAiAssetUseCase.reviewAiAsset(new ReviewAiAssetCommand(
                107L,
                asset.getId(),
                "ADMIN",
                "REVIEWER",
                "REJECT",
                "not acceptable",
                false,
                BASE_TIME.plusMinutes(20)
        ));

        assertThat(rejected.status()).isEqualTo("REJECTED");
        assertThat(generatedAssetRepository.findById(asset.getId()))
                .map(AiGeneratedAsset::getStatus)
                .contains(AiGeneratedAssetStatus.REJECTED);
        assertThat(auditLogClientMock.writtenCommands())
                .anySatisfy(command -> assertThat(command.actionType()).isEqualTo("AI_ASSET_REJECT"));
    }

    private void assertAdminRegenerateRequestedOutbox(Long jobId, Long promptVersionId) throws Exception {
        List<AiEventOutbox> events = eventOutboxRepository.findAll();
        assertThat(events)
                .singleElement()
                .satisfies(event -> {
                    assertThat(event.getEventName()).isEqualTo("AiGenerationJobRequested");
                    assertThat(event.getAggregateType()).isEqualTo("ai_generation_job");
                    assertThat(event.getAggregateId()).isEqualTo("job-" + jobId);
                    assertThat(event.getSchemaVersion()).isEqualTo("0.1.0");
                    assertThat(event.getStatus()).isEqualTo(AiEventOutboxStatus.PENDING);
                });
        JsonNode payload = objectMapper.readTree(events.getFirst().getPayloadJson());
        assertThat(payload.path("jobId").asLong()).isEqualTo(jobId);
        assertThat(payload.path("jobType").asText()).isEqualTo("EXPLANATION");
        assertThat(payload.path("targetType").asText()).isEqualTo("BIBLE_VERSE");
        assertThat(payload.path("targetId").asLong()).isEqualTo(501L);
        assertThat(payload.has("passageId")).isFalse();
        assertThat(payload.path("promptVersionId").asLong()).isEqualTo(promptVersionId);
        assertThat(payload.path("requestedBy").asText()).isEqualTo("ADMIN:107");
        assertThat(payload.path("requestSource").asText()).isEqualTo("admin-ai-regenerate");
        assertThat(payload.path("requestedAt").asText()).isEqualTo("2026-06-09T09:12:00+09:00");
    }

    private AiGeneratedAsset saveExplanationVerseAsset(AiPromptVersion promptVersion, Long verseId) {
        AiGenerationJob job = generationJobRepository.saveAndFlush(AiGenerationJob.queue(
                AiGenerationJobType.EXPLANATION,
                AiTargetType.BIBLE_VERSE,
                verseId,
                promptVersion.getId(),
                BASE_TIME
        ));
        job.markRunning(BASE_TIME.plusSeconds(1));
        job.markSucceeded(BASE_TIME.plusSeconds(2));
        generationJobRepository.saveAndFlush(job);
        return generatedAssetRepository.saveAndFlush(AiGeneratedAsset.create(
                job.getId(),
                AiGeneratedAssetType.EXPLANATION,
                AiTargetType.BIBLE_VERSE,
                verseId,
                """
                        {"explanations":[{"verseId":%d,"summary":"Allowed summary","explanation":"Allowed explanation"}]}
                        """.formatted(verseId),
                "QT-AI",
                BASE_TIME.plusMinutes(1)
        ));
    }

    private void savePassedValidation(
            AiGeneratedAsset asset,
            AiValidationChecklistVersion checklistVersion,
            int layer,
            AiValidationReviewerType reviewerType
    ) {
        validationLogRepository.saveAndFlush(AiValidationLog.create(
                asset.getId(),
                null,
                layer,
                AiValidationResult.PASSED,
                reviewerType,
                checklistVersion.getId(),
                "{\"checked\":true}",
                null,
                BASE_TIME.plusMinutes(layer)
        ));
    }

    private static AiPromptVersion promptVersion(Long id, String version) {
        return AiPromptVersion.of(
                id,
                AiPromptType.EXPLANATION,
                version,
                "sha256:" + version,
                AiPromptVersionStatus.ACTIVE,
                BASE_TIME.minusMinutes(1)
        );
    }

    private static AiValidationChecklistVersion checklistVersion() {
        return AiValidationChecklistVersion.create(
                AiValidationChecklistType.EXPLANATION,
                "2026.06.review",
                "sha256:review-checklist",
                107L,
                BASE_TIME.minusMinutes(1)
        );
    }
}
