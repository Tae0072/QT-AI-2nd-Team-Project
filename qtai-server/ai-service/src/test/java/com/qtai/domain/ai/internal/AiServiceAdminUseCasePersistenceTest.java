package com.qtai.domain.ai.internal;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.qtai.ai.AiServiceApplication;
import com.qtai.domain.ai.api.admin.asset.GetAdminAiAssetUseCase;
import com.qtai.domain.ai.api.admin.asset.ListAdminAiAssetsUseCase;
import com.qtai.domain.ai.api.admin.asset.dto.GetAdminAiAssetQuery;
import com.qtai.domain.ai.api.admin.asset.dto.ListAdminAiAssetsQuery;
import com.qtai.domain.ai.api.admin.checklist.ActivateAdminAiValidationChecklistUseCase;
import com.qtai.domain.ai.api.admin.checklist.CreateAdminAiValidationChecklistUseCase;
import com.qtai.domain.ai.api.admin.checklist.ListAdminAiValidationChecklistsUseCase;
import com.qtai.domain.ai.api.admin.checklist.RetireAdminAiValidationChecklistUseCase;
import com.qtai.domain.ai.api.admin.checklist.dto.ChangeAdminAiValidationChecklistStatusCommand;
import com.qtai.domain.ai.api.admin.checklist.dto.CreateAdminAiValidationChecklistCommand;
import com.qtai.domain.ai.api.admin.checklist.dto.ListAdminAiValidationChecklistsQuery;
import com.qtai.domain.ai.api.admin.monitoring.GetAdminAiMonitoringUseCase;
import com.qtai.domain.ai.api.admin.monitoring.ListAdminAiBatchRunLogsUseCase;
import com.qtai.domain.ai.api.admin.monitoring.dto.GetAdminAiMonitoringQuery;
import com.qtai.domain.ai.api.admin.monitoring.dto.ListAdminAiBatchRunLogsQuery;

@SpringBootTest(
        classes = AiServiceApplication.class,
        properties = {
                "qtai.ai.inbound.enabled=true",
                "qtai.ai.persistence.enabled=true",
                "qtai.ai.persistence.url=jdbc:h2:mem:ai_service_admin_usecase;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
                "qtai.ai.persistence.username=sa",
                "qtai.ai.persistence.driver-class-name=org.h2.Driver",
                "qtai.ai.persistence.ddl-auto=create-drop",
                "qtai.ai.persistence.flyway-enabled=false"
        }
)
@ActiveProfiles("test")
class AiServiceAdminUseCasePersistenceTest {

    private static final OffsetDateTime BASE_TIME = OffsetDateTime.parse("2026-06-09T09:00:00+09:00");

    @Autowired
    private ListAdminAiAssetsUseCase listAdminAiAssetsUseCase;
    @Autowired
    private GetAdminAiAssetUseCase getAdminAiAssetUseCase;
    @Autowired
    private ListAdminAiValidationChecklistsUseCase listChecklistsUseCase;
    @Autowired
    private CreateAdminAiValidationChecklistUseCase createChecklistUseCase;
    @Autowired
    private ActivateAdminAiValidationChecklistUseCase activateChecklistUseCase;
    @Autowired
    private RetireAdminAiValidationChecklistUseCase retireChecklistUseCase;
    @Autowired
    private GetAdminAiMonitoringUseCase monitoringUseCase;
    @Autowired
    private ListAdminAiBatchRunLogsUseCase batchRunLogsUseCase;
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
    private AiBatchRunLogRepository batchRunLogRepository;

    @Test
    void adminUseCasesReadAndMutateOwnedAiModels() {
        AiPromptVersion promptVersion = promptVersionRepository.saveAndFlush(promptVersion());
        AiGenerationJob job = generationJobRepository.saveAndFlush(AiGenerationJob.queue(
                AiGenerationJobType.EXPLANATION,
                AiTargetType.QT_PASSAGE,
                35L,
                promptVersion.getId(),
                BASE_TIME
        ));
        AiGeneratedAsset asset = generatedAssetRepository.saveAndFlush(AiGeneratedAsset.create(
                job.getId(),
                AiGeneratedAssetType.EXPLANATION,
                AiTargetType.QT_PASSAGE,
                35L,
                "{\"summary\":\"Allowed admin summary\"}",
                "QT-AI",
                BASE_TIME.plusMinutes(1)
        ));
        AiValidationChecklistVersion checklistVersion = checklistVersionRepository.saveAndFlush(
                AiValidationChecklistVersion.create(
                        AiValidationChecklistType.EXPLANATION,
                        "2026.06.admin",
                        "sha256:admin-checklist",
                        107L,
                        BASE_TIME.plusMinutes(2)
                )
        );
        validationLogRepository.saveAndFlush(AiValidationLog.create(
                asset.getId(),
                null,
                1,
                AiValidationResult.PASSED,
                AiValidationReviewerType.AUTO,
                checklistVersion.getId(),
                "{\"checked\":true}",
                null,
                BASE_TIME.plusMinutes(3)
        ));
        batchRunLogRepository.saveAndFlush(AiBatchRunLog.create(new AiBatchRunLogCommand(
                AiBatchName.AI_GENERATION_WORKER_POLL,
                AiBatchRunStatus.SUCCEEDED,
                1,
                0,
                1,
                null,
                null,
                BASE_TIME.plusMinutes(4),
                BASE_TIME.plusMinutes(5)
        )));

        var list = listAdminAiAssetsUseCase.listAdminAiAssets(new ListAdminAiAssetsQuery(
                107L,
                "ADMIN",
                "REVIEWER",
                "EXPLANATION",
                "QT_PASSAGE",
                "VALIDATING",
                promptVersion.getId(),
                checklistVersion.getId(),
                0,
                20
        ));
        assertThat(list.totalElements()).isEqualTo(1);
        assertThat(list.content()).extracting("id").containsExactly(asset.getId());

        var detail = getAdminAiAssetUseCase.getAdminAiAsset(new GetAdminAiAssetQuery(
                107L,
                "ADMIN",
                "REVIEWER",
                asset.getId()
        ));
        assertThat(detail.id()).isEqualTo(asset.getId());
        assertThat(detail.validationLogs()).hasSize(1);

        var createdChecklist = createChecklistUseCase.createAdminAiValidationChecklist(
                new CreateAdminAiValidationChecklistCommand(
                        107L,
                        "ADMIN",
                        "REVIEWER",
                        "EXPLANATION",
                        "2026.06.admin-created",
                        "sha256:admin-created-checklist",
                        "DRAFT"
                )
        );
        assertThat(createdChecklist.status()).isEqualTo("DRAFT");
        assertThat(listChecklistsUseCase.listAdminAiValidationChecklists(
                new ListAdminAiValidationChecklistsQuery(107L, "ADMIN", "REVIEWER", "EXPLANATION", null, 0, 20))
                .totalElements()).isGreaterThanOrEqualTo(2);

        var activated = activateChecklistUseCase.activateAdminAiValidationChecklist(
                new ChangeAdminAiValidationChecklistStatusCommand(107L, "ADMIN", "REVIEWER", createdChecklist.id()));
        assertThat(activated.status()).isEqualTo("ACTIVE");

        var monitoring = monitoringUseCase.getAdminAiMonitoring(
                new GetAdminAiMonitoringQuery(107L, "ADMIN", "REVIEWER", "2026-06-09", "2026-06-09"));
        assertThat(monitoring.generationJobs().queued()).isEqualTo(1);
        assertThat(monitoring.validation().passCount()).isEqualTo(1);
        assertThat(monitoring.checklists()).isNotEmpty();

        var batchRunLogs = batchRunLogsUseCase.listAdminAiBatchRunLogs(new ListAdminAiBatchRunLogsQuery(
                107L,
                "ADMIN",
                "REVIEWER",
                "AI_GENERATION_WORKER_POLL",
                "SUCCEEDED",
                null,
                null,
                0,
                20
        ));
        assertThat(batchRunLogs.totalElements()).isEqualTo(1);

        var retired = retireChecklistUseCase.retireAdminAiValidationChecklist(
                new ChangeAdminAiValidationChecklistStatusCommand(107L, "ADMIN", "REVIEWER", createdChecklist.id()));
        assertThat(retired.status()).isEqualTo("RETIRED");
    }

    private static AiPromptVersion promptVersion() {
        return AiPromptVersion.of(
                2L,
                AiPromptType.EXPLANATION,
                "2026.06.admin",
                "sha256:admin-prompt",
                AiPromptVersionStatus.ACTIVE,
                BASE_TIME.minusMinutes(1)
        );
    }
}
