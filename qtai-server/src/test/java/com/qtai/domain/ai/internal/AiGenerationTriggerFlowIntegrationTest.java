package com.qtai.domain.ai.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.qtai.common.exception.BusinessException;
import com.qtai.common.exception.ErrorCode;
import com.qtai.domain.ai.api.CreateAiGenerationJobUseCase;
import com.qtai.domain.ai.api.RegenerateAiAssetUseCase;
import com.qtai.domain.ai.api.dto.CreateAiGenerationJobCommand;
import com.qtai.domain.ai.api.dto.CreateAiGenerationJobResult;
import com.qtai.domain.ai.api.dto.RegenerateAiAssetCommand;
import com.qtai.domain.ai.api.dto.RegenerateAiAssetResult;
import com.qtai.domain.bible.api.GetBibleVerseUseCase;
import com.qtai.domain.bible.api.dto.BibleVerseResponse;
import com.qtai.domain.qt.api.GetQtPassageContentContextUseCase;
import com.qtai.domain.qt.api.dto.QtPassageContentContext;
import com.qtai.external.llm.LlmClient;
import com.qtai.external.llm.dto.LlmCompletionResponse;

@DataJpaTest
@ActiveProfiles("test")
class AiGenerationTriggerFlowIntegrationTest {

    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-06-01T00:00:00Z"),
            ZoneId.of("Asia/Seoul")
    );
    private static final OffsetDateTime BASE_TIME = OffsetDateTime.parse("2026-06-01T09:00:00+09:00");

    @Autowired
    private AiGenerationJobRepository generationJobRepository;

    @Autowired
    private AiGeneratedAssetRepository generatedAssetRepository;

    @Autowired
    private AiValidationLogRepository validationLogRepository;

    @Autowired
    private AiValidationChecklistVersionRepository checklistVersionRepository;

    @Autowired
    private AiPromptVersionRepository promptVersionRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private GetQtPassageContentContextUseCase getQtPassageContentContextUseCase;
    private GetBibleVerseUseCase getBibleVerseUseCase;
    private LlmClient llmClient;
    private ObjectMapper objectMapper;
    private AiService aiService;
    private AiGenerationJobRunner runner;

    @BeforeEach
    void setUp() {
        getQtPassageContentContextUseCase = mock(GetQtPassageContentContextUseCase.class);
        getBibleVerseUseCase = mock(GetBibleVerseUseCase.class);
        llmClient = mock(LlmClient.class);
        objectMapper = new ObjectMapper();
        aiService = new AiService(generationJobRepository, generatedAssetRepository, promptVersionRepository);
        runner = new AiGenerationJobRunner(
                generationJobRepository,
                generatedAssetRepository,
                autoValidationService(),
                List.of(explanationHandler()),
                CLOCK,
                new TransactionTemplate(transactionManager)
        );
    }

    @Test
    void systemGenerationJobTriggerIsProcessedThroughRunnerToAssetAndValidationLog() {
        AiPromptVersion promptVersion = persistPromptVersion(AiPromptType.EXPLANATION);
        AiValidationChecklistVersion checklistVersion = persistActiveChecklistVersion();
        givenQtPassageContext(35L, List.of(1001L, 1002L));
        givenBibleVerses();
        givenLlmResponseForVerseIds(1001L, 1002L);

        CreateAiGenerationJobResult result = ((CreateAiGenerationJobUseCase) aiService).createAiGenerationJob(
                new CreateAiGenerationJobCommand(
                        "EXPLANATION",
                        "QT_PASSAGE",
                        35L,
                        promptVersion.getId(),
                        "SYSTEM_BATCH",
                        BASE_TIME
                )
        );

        AiGenerationJob queuedJob = generationJobRepository.findById(result.generationJobId()).orElseThrow();
        assertThat(result.status()).isEqualTo("QUEUED");
        assertThat(queuedJob.getStatus()).isEqualTo(AiGenerationJobStatus.QUEUED);

        assertThat(runner.runQueuedBatch(5)).isEqualTo(1);

        AiGenerationJob completedJob = generationJobRepository.findById(result.generationJobId()).orElseThrow();
        List<AiGeneratedAsset> assets = generatedAssetRepository.findAll();
        List<AiValidationLog> validationLogs = validationLogRepository.findAll();
        assertThat(completedJob.getStatus()).isEqualTo(AiGenerationJobStatus.SUCCEEDED);
        assertThat(completedJob.getErrorMessage()).isNull();
        assertThat(assets).hasSize(1);
        AiGeneratedAsset asset = assets.get(0);
        assertThat(asset.getGenerationJobId()).isEqualTo(completedJob.getId());
        assertThat(asset.getAssetType()).isEqualTo(AiGeneratedAssetType.EXPLANATION);
        assertThat(asset.getTargetType()).isEqualTo(AiTargetType.QT_PASSAGE);
        assertThat(asset.getTargetId()).isEqualTo(35L);
        assertThat(asset.getStatus()).isEqualTo(AiGeneratedAssetStatus.VALIDATING);
        assertThat(validationLogs).hasSize(1);
        assertThat(validationLogs.get(0).getAiAssetId()).isEqualTo(asset.getId());
        assertThat(validationLogs.get(0).getChecklistVersionId()).isEqualTo(checklistVersion.getId());
        assertThat(validationLogs.get(0).getResult()).isEqualTo(AiValidationResult.PASSED);
    }

    @Test
    void adminRegenerationTriggerPreservesExistingAssetAndCreatesNewProcessedAsset() {
        AiPromptVersion promptVersion = persistPromptVersion(AiPromptType.EXPLANATION);
        AiValidationChecklistVersion checklistVersion = persistActiveChecklistVersion();
        AiGeneratedAsset rejectedAsset = persistRejectedAsset(AiTargetType.BIBLE_VERSE, 1001L);
        givenBibleVerses();
        givenLlmResponseForVerseIds(1001L);

        RegenerateAiAssetResult result = ((RegenerateAiAssetUseCase) aiService).regenerateAiAsset(
                new RegenerateAiAssetCommand(
                        7L,
                        rejectedAsset.getId(),
                        "ADMIN",
                        "REVIEWER",
                        "regenerate rejected explanation",
                        promptVersion.getId(),
                        BASE_TIME.plusMinutes(1)
                )
        );

        AiGenerationJob queuedJob = generationJobRepository.findById(result.generationJobId()).orElseThrow();
        assertThat(result.status()).isEqualTo("QUEUED");
        assertThat(queuedJob.getStatus()).isEqualTo(AiGenerationJobStatus.QUEUED);
        assertThat(generatedAssetRepository.findById(rejectedAsset.getId()).orElseThrow().getStatus())
                .isEqualTo(AiGeneratedAssetStatus.REJECTED);

        assertThat(runner.runQueuedBatch(5)).isEqualTo(1);

        AiGenerationJob completedJob = generationJobRepository.findById(result.generationJobId()).orElseThrow();
        List<AiGeneratedAsset> assets = generatedAssetRepository.findAll().stream()
                .sorted(Comparator.comparing(AiGeneratedAsset::getId))
                .toList();
        List<AiValidationLog> validationLogs = validationLogRepository.findAll();
        assertThat(completedJob.getStatus()).isEqualTo(AiGenerationJobStatus.SUCCEEDED);
        assertThat(assets).hasSize(2);
        assertThat(assets.get(0).getId()).isEqualTo(rejectedAsset.getId());
        assertThat(assets.get(0).getStatus()).isEqualTo(AiGeneratedAssetStatus.REJECTED);
        AiGeneratedAsset regeneratedAsset = assets.get(1);
        assertThat(regeneratedAsset.getGenerationJobId()).isEqualTo(completedJob.getId());
        assertThat(regeneratedAsset.getTargetType()).isEqualTo(AiTargetType.BIBLE_VERSE);
        assertThat(regeneratedAsset.getTargetId()).isEqualTo(1001L);
        assertThat(regeneratedAsset.getStatus()).isEqualTo(AiGeneratedAssetStatus.VALIDATING);
        assertThat(validationLogs).hasSize(1);
        assertThat(validationLogs.get(0).getAiAssetId()).isEqualTo(regeneratedAsset.getId());
        assertThat(validationLogs.get(0).getChecklistVersionId()).isEqualTo(checklistVersion.getId());
        assertThat(validationLogs.get(0).getResult()).isEqualTo(AiValidationResult.PASSED);
    }

    @Test
    void adminRegenerationTriggerIsBlockedWhenActiveJobAlreadyExistsForSameTargetAndPromptVersion() {
        AiPromptVersion promptVersion = persistPromptVersion(AiPromptType.EXPLANATION);
        AiGeneratedAsset rejectedAsset = persistRejectedAsset(AiTargetType.BIBLE_VERSE, 1001L);
        generationJobRepository.saveAndFlush(AiGenerationJob.queue(
                AiGenerationJobType.EXPLANATION,
                AiTargetType.BIBLE_VERSE,
                1001L,
                promptVersion.getId(),
                BASE_TIME
        ));

        assertThatThrownBy(() -> ((RegenerateAiAssetUseCase) aiService).regenerateAiAsset(
                new RegenerateAiAssetCommand(
                        7L,
                        rejectedAsset.getId(),
                        "ADMIN",
                        "REVIEWER",
                        "duplicate active job check",
                        promptVersion.getId(),
                        BASE_TIME.plusMinutes(1)
                )
        )).isInstanceOfSatisfying(BusinessException.class, exception ->
                assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_STATUS_TRANSITION));

        assertThat(generationJobRepository.findAll()).hasSize(1);
        assertThat(generatedAssetRepository.findAll()).hasSize(1);
    }

    private AiAutoValidationService autoValidationService() {
        AiLogService aiLogService = new AiLogService(
                generationJobRepository,
                generatedAssetRepository,
                validationLogRepository
        );
        return new AiAutoValidationService(
                generatedAssetRepository,
                checklistVersionRepository,
                aiLogService,
                objectMapper
        );
    }

    private ExplanationGenerationJobHandler explanationHandler() {
        return new ExplanationGenerationJobHandler(
                getQtPassageContentContextUseCase,
                getBibleVerseUseCase,
                promptVersionRepository,
                llmClient,
                objectMapper
        );
    }

    private AiPromptVersion persistPromptVersion(AiPromptType promptType) {
        AiPromptVersion promptVersion = new AiPromptVersion();
        setField(promptVersion, "promptType", promptType);
        setField(promptVersion, "version", "2026.06." + promptType);
        setField(promptVersion, "contentHash", "hash-" + promptType);
        setField(promptVersion, "status", AiPromptVersionStatus.ACTIVE);
        setField(promptVersion, "createdAt", BASE_TIME.minusDays(1));
        return promptVersionRepository.saveAndFlush(promptVersion);
    }

    private AiValidationChecklistVersion persistActiveChecklistVersion() {
        AiValidationChecklistVersion checklistVersion = AiValidationChecklistVersion.create(
                AiValidationChecklistType.EXPLANATION,
                "2026.06.01",
                "sha256:trigger-flow-check",
                null,
                BASE_TIME.minusDays(1)
        );
        checklistVersion.activate(BASE_TIME.minusHours(1));
        return checklistVersionRepository.saveAndFlush(checklistVersion);
    }

    private AiGeneratedAsset persistRejectedAsset(AiTargetType targetType, Long targetId) {
        AiGeneratedAsset asset = AiGeneratedAsset.create(
                900L,
                AiGeneratedAssetType.EXPLANATION,
                targetType,
                targetId,
                """
                        {
                          "explanations": [
                            {"verseId": 1001, "summary": "old", "explanation": "old explanation"}
                          ],
                          "glossaryTerms": [],
                          "sourceMetadata": {"verseIds": [1001]}
                        }
                        """,
                "QT-AI previous generated content",
                BASE_TIME.minusHours(2)
        );
        asset.reject(BASE_TIME.minusHours(1));
        return generatedAssetRepository.saveAndFlush(asset);
    }

    private void givenQtPassageContext(Long qtPassageId, List<Long> verseIds) {
        when(getQtPassageContentContextUseCase.getContentContext(qtPassageId))
                .thenReturn(new QtPassageContentContext(
                        qtPassageId,
                        LocalDate.of(2026, 6, 1),
                        "QT trigger flow",
                        verseIds,
                        true
                ));
    }

    private void givenBibleVerses() {
        when(getBibleVerseUseCase.getVerses(any()))
                .thenAnswer(invocation -> {
                    List<Long> verseIds = invocation.getArgument(0);
                    return verseIds.stream()
                            .map(verseId -> new BibleVerseResponse(
                                    verseId,
                                    "TST",
                                    1,
                                    verseId.intValue(),
                                    "neutral text " + verseId,
                                    null
                            ))
                            .toList();
                });
    }

    private void givenLlmResponseForVerseIds(Long... verseIds) {
        StringBuilder explanations = new StringBuilder();
        for (int index = 0; index < verseIds.length; index++) {
            if (index > 0) {
                explanations.append(',');
            }
            explanations.append("""
                    {"verseId": %d, "summary": "summary %d", "explanation": "explanation %d"}
                    """.formatted(verseIds[index], verseIds[index], verseIds[index]));
        }
        when(llmClient.complete(any())).thenReturn(new LlmCompletionResponse(
                """
                        {
                          "explanations": [%s],
                          "glossaryTerms": []
                        }
                        """.formatted(explanations),
                11,
                22,
                33,
                "deepseek-test"
        ));
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
