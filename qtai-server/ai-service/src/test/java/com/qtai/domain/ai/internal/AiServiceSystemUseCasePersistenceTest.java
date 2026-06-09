package com.qtai.domain.ai.internal;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.qtai.ai.AiServiceApplication;
import com.qtai.domain.ai.api.generation.CreateAiGenerationJobUseCase;
import com.qtai.domain.ai.api.generation.RegisterAiGeneratedAssetUseCase;
import com.qtai.domain.ai.api.generation.dto.CreateAiGenerationJobCommand;
import com.qtai.domain.ai.api.generation.dto.RegisterAiGeneratedAssetCommand;
import com.qtai.domain.ai.api.validation.CreateValidationReferenceJobUseCase;
import com.qtai.domain.ai.api.validation.ExpireValidationReferenceJobUseCase;
import com.qtai.domain.ai.api.validation.GetValidationReferenceJobUseCase;
import com.qtai.domain.ai.api.validation.RegisterAiValidationLogUseCase;
import com.qtai.domain.ai.api.validation.dto.CreateValidationReferenceJobCommand;
import com.qtai.domain.ai.api.validation.dto.ExpireValidationReferenceJobCommand;
import com.qtai.domain.ai.api.validation.dto.GetValidationReferenceJobQuery;
import com.qtai.domain.ai.api.validation.dto.RegisterAiValidationLogCommand;
import com.qtai.domain.ai.client.audit.AuditLogClientMock;

@SpringBootTest(
        classes = AiServiceApplication.class,
        properties = {
                "qtai.ai.inbound.enabled=true",
                "qtai.ai.persistence.enabled=true",
                "qtai.ai.persistence.url=jdbc:h2:mem:ai_service_system_usecase;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
                "qtai.ai.persistence.username=sa",
                "qtai.ai.persistence.driver-class-name=org.h2.Driver",
                "qtai.ai.persistence.ddl-auto=create-drop",
                "qtai.ai.persistence.flyway-enabled=false"
        }
)
@ActiveProfiles("test")
class AiServiceSystemUseCasePersistenceTest {

    private static final OffsetDateTime BASE_TIME = OffsetDateTime.parse("2026-06-09T09:00:00+09:00");

    @Autowired
    private CreateAiGenerationJobUseCase createAiGenerationJobUseCase;
    @Autowired
    private RegisterAiGeneratedAssetUseCase registerAiGeneratedAssetUseCase;
    @Autowired
    private RegisterAiValidationLogUseCase registerAiValidationLogUseCase;
    @Autowired
    private CreateValidationReferenceJobUseCase createValidationReferenceJobUseCase;
    @Autowired
    private GetValidationReferenceJobUseCase getValidationReferenceJobUseCase;
    @Autowired
    private ExpireValidationReferenceJobUseCase expireValidationReferenceJobUseCase;
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
    private ValidationReferenceJobRepository validationReferenceJobRepository;
    @Autowired
    private AuditLogClientMock auditLogClientMock;

    @Test
    void systemUseCasesPersistOwnedAiModels() {
        AiPromptVersion promptVersion = promptVersionRepository.saveAndFlush(promptVersion());
        AiValidationChecklistVersion checklistVersion = checklistVersionRepository.saveAndFlush(checklistVersion());

        var jobResult = createAiGenerationJobUseCase.createAiGenerationJob(new CreateAiGenerationJobCommand(
                "EXPLANATION",
                "QT_PASSAGE",
                35L,
                promptVersion.getId(),
                "SYSTEM_BATCH",
                BASE_TIME
        ));
        assertThat(jobResult.status()).isEqualTo("QUEUED");
        assertThat(generationJobRepository.findById(jobResult.generationJobId()))
                .map(AiGenerationJob::getTargetId)
                .contains(35L);

        var assetResult = registerAiGeneratedAssetUseCase.registerAiGeneratedAsset(new RegisterAiGeneratedAssetCommand(
                jobResult.generationJobId(),
                "EXPLANATION",
                "QT_PASSAGE",
                35L,
                "{\"summary\":\"Allowed system summary\"}",
                "QT-AI",
                BASE_TIME.plusMinutes(1)
        ));
        assertThat(assetResult.status()).isEqualTo("VALIDATING");
        assertThat(generatedAssetRepository.findById(assetResult.assetId()))
                .map(AiGeneratedAsset::getSourceLabel)
                .contains("QT-AI");

        var reference = createValidationReferenceJobUseCase.createValidationReferenceJob(
                new CreateValidationReferenceJobCommand(
                        "review-reference",
                        "reference.pdf",
                        "sha256:reference",
                        "s3://ai-service-test/reference.pdf",
                        "s3://ai-service-test/reference-index.json",
                        BASE_TIME.plusDays(7)
                )
        );
        assertThat(reference.status()).isEqualTo("ACTIVE");
        assertThat(validationReferenceJobRepository.findById(reference.id()))
                .map(ValidationReferenceJob::getStatus)
                .contains(ValidationReferenceJobStatus.ACTIVE);

        var validationLogResult = registerAiValidationLogUseCase.registerAiValidationLog(
                new RegisterAiValidationLogCommand(
                        assetResult.assetId(),
                        reference.id(),
                        1,
                        "PASSED",
                        "AUTO",
                        checklistVersion.getId(),
                        "{\"checked\":true}",
                        null,
                        BASE_TIME.plusMinutes(2)
                )
        );
        assertThat(validationLogResult.result()).isEqualTo("PASSED");
        assertThat(validationLogResult.assetStatus()).isEqualTo("VALIDATING");
        assertThat(validationLogRepository.findById(validationLogResult.validationLogId()))
                .map(AiValidationLog::getReviewerType)
                .contains(AiValidationReviewerType.AUTO);

        assertThat(getValidationReferenceJobUseCase.getValidationReferenceJob(
                new GetValidationReferenceJobQuery(reference.id())).id()).isEqualTo(reference.id());
        assertThat(expireValidationReferenceJobUseCase.expireValidationReferenceJob(
                new ExpireValidationReferenceJobCommand(reference.id())).status()).isEqualTo("EXPIRED");
        assertThat(auditLogClientMock.writtenCommands())
                .anySatisfy(command -> assertThat(command.actorType()).isEqualTo("SYSTEM_BATCH"));
    }

    private static AiPromptVersion promptVersion() {
        return AiPromptVersion.of(
                1L,
                AiPromptType.EXPLANATION,
                "2026.06.system",
                "sha256:system-prompt",
                AiPromptVersionStatus.ACTIVE,
                BASE_TIME.minusMinutes(1)
        );
    }

    private static AiValidationChecklistVersion checklistVersion() {
        return AiValidationChecklistVersion.create(
                AiValidationChecklistType.EXPLANATION,
                "2026.06.system",
                "sha256:system-checklist",
                107L,
                BASE_TIME.minusMinutes(1)
        );
    }
}
