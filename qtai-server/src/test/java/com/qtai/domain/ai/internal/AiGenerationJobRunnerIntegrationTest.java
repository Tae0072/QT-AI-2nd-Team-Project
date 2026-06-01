package com.qtai.domain.ai.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.qtai.domain.bible.api.GetBibleVerseUseCase;
import com.qtai.domain.bible.api.dto.BibleVerseResponse;
import com.qtai.domain.qt.api.GetQtPassageContentContextUseCase;
import com.qtai.domain.qt.api.dto.QtPassageContentContext;
import com.qtai.external.llm.LlmClient;
import com.qtai.external.llm.dto.LlmCompletionRequest;
import com.qtai.external.llm.dto.LlmCompletionResponse;

@DataJpaTest
@ActiveProfiles("test")
class AiGenerationJobRunnerIntegrationTest {

    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-05-29T00:00:00Z"),
            ZoneId.of("Asia/Seoul")
    );
    private static final OffsetDateTime BASE_TIME = OffsetDateTime.parse("2026-05-29T04:00:00+09:00");

    @Autowired
    private TestEntityManager testEntityManager;

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

    @BeforeEach
    void setUp() {
        getQtPassageContentContextUseCase = mock(GetQtPassageContentContextUseCase.class);
        getBibleVerseUseCase = mock(GetBibleVerseUseCase.class);
        llmClient = mock(LlmClient.class);
        objectMapper = new ObjectMapper();
    }

    @Test
    void explanationJobStoresValidatingAssetAndSucceeds() throws Exception {
        AiPromptVersion promptVersion = persistPromptVersion(AiPromptType.EXPLANATION);
        AiValidationChecklistVersion checklistVersion = persistActiveChecklistVersion();
        AiGenerationJob job = persistJob(AiGenerationJobType.EXPLANATION, promptVersion);
        givenQtAndBibleContext(List.of(1001L, 1002L));
        when(llmClient.complete(any())).thenReturn(completionResponse("""
                {
                  "explanations": [
                    {"verseId": 1001, "summary": "summary one", "explanation": "explanation one"},
                    {"verseId": 1002, "summary": "summary two", "explanation": "explanation two"}
                  ],
                  "glossaryTerms": [
                    {"verseId": 1001, "term": "context", "meaning": "background meaning"}
                  ]
                }
                """));
        AiGenerationJobRunner runner = runner(explanationHandler(), new SimulatorGenerationJobHandler());

        assertThat(runner.runJob(job.getId())).isTrue();
        flushAndClear();

        AiGenerationJob foundJob = generationJobRepository.findById(job.getId()).orElseThrow();
        List<AiGeneratedAsset> assets = generatedAssetRepository.findAll();
        List<AiValidationLog> validationLogs = validationLogRepository.findAll();
        assertThat(foundJob.getStatus()).isEqualTo(AiGenerationJobStatus.SUCCEEDED);
        assertThat(foundJob.getErrorMessage()).isNull();
        assertThat(assets).hasSize(1);
        AiGeneratedAsset asset = assets.get(0);
        assertThat(asset.getGenerationJobId()).isEqualTo(job.getId());
        assertThat(asset.getAssetType()).isEqualTo(AiGeneratedAssetType.EXPLANATION);
        assertThat(asset.getStatus()).isEqualTo(AiGeneratedAssetStatus.VALIDATING);

        JsonNode payload = objectMapper.readTree(asset.getPayloadJson());
        assertThat(payload.path("explanations")).hasSize(2);
        assertThat(payload.path("glossaryTerms")).hasSize(1);
        assertThat(payload.path("promptVersionId").asLong()).isEqualTo(promptVersion.getId());
        assertThat(payload.path("promptVersion").asText()).isEqualTo(promptVersion.getVersion());
        assertThat(payload.path("promptContentHash").asText()).isEqualTo(promptVersion.getContentHash());
        assertThat(payload.path("modelName").asText()).isEqualTo("deepseek-test");
        assertThat(payload.path("tokenUsage").path("totalTokens").asInt()).isEqualTo(33);
        assertThat(payload.path("sourceMetadata").path("verseIds")).hasSize(2);
        assertThat(asset.getPayloadJson()).doesNotContain(
                "providerRawResponse",
                "rawResponse",
                "validationReferenceText",
                "promptText"
        );
        assertThat(validationLogs).hasSize(1);
        AiValidationLog validationLog = validationLogs.get(0);
        assertThat(validationLog.getAiAssetId()).isEqualTo(asset.getId());
        assertThat(validationLog.getResult()).isEqualTo(AiValidationResult.PASSED);
        assertThat(validationLog.getReviewerType()).isEqualTo(AiValidationReviewerType.AUTO);
        assertThat(validationLog.getLayer()).isEqualTo(1);
        assertThat(validationLog.getValidationReferenceJobId()).isNull();
        assertThat(validationLog.getChecklistVersionId()).isEqualTo(checklistVersion.getId());
        assertThat(validationLog.getChecklistJson()).contains("AI_AUTO_VALIDATION_MINIMUM", "PASSED");
    }

    @Test
    void autoValidationFailureRejectsAssetAndSucceedsJob() {
        AiPromptVersion promptVersion = persistPromptVersion(AiPromptType.EXPLANATION);
        AiValidationChecklistVersion checklistVersion = persistActiveChecklistVersion();
        AiGenerationJob job = persistJob(AiGenerationJobType.EXPLANATION, promptVersion);
        AiGenerationJobRunner runner = runner(payloadHandler("""
                {
                  "explanations": [],
                  "glossaryTerms": [],
                  "sourceMetadata": {"verseIds": [1001]}
                }
                """));

        assertThat(runner.runJob(job.getId())).isTrue();
        flushAndClear();

        AiGenerationJob foundJob = generationJobRepository.findById(job.getId()).orElseThrow();
        List<AiGeneratedAsset> assets = generatedAssetRepository.findAll();
        List<AiValidationLog> validationLogs = validationLogRepository.findAll();
        assertThat(foundJob.getStatus()).isEqualTo(AiGenerationJobStatus.SUCCEEDED);
        assertThat(foundJob.getErrorMessage()).isNull();
        assertThat(assets).hasSize(1);
        AiGeneratedAsset asset = assets.get(0);
        assertThat(asset.getStatus()).isEqualTo(AiGeneratedAssetStatus.REJECTED);
        assertThat(validationLogs).hasSize(1);
        AiValidationLog validationLog = validationLogs.get(0);
        assertThat(validationLog.getAiAssetId()).isEqualTo(asset.getId());
        assertThat(validationLog.getResult()).isEqualTo(AiValidationResult.REJECTED);
        assertThat(validationLog.getReviewerType()).isEqualTo(AiValidationReviewerType.AUTO);
        assertThat(validationLog.getChecklistVersionId()).isEqualTo(checklistVersion.getId());
        assertThat(validationLog.getErrorMessage()).isEqualTo("EXPLANATION_SCHEMA");
    }

    @Test
    void autoValidationConfigurationErrorFailsJobWithoutValidationLog() {
        AiPromptVersion promptVersion = persistPromptVersion(AiPromptType.EXPLANATION);
        AiGenerationJob job = persistJob(AiGenerationJobType.EXPLANATION, promptVersion);
        AiGenerationJobRunner runner = runner(payloadHandler("""
                {
                  "explanations": [
                    {"verseId": 1001, "summary": "summary", "explanation": "explanation"}
                  ],
                  "glossaryTerms": [],
                  "sourceMetadata": {"verseIds": [1001]}
                }
                """));

        assertThat(runner.runJob(job.getId())).isTrue();
        flushAndClear();

        AiGenerationJob foundJob = generationJobRepository.findById(job.getId()).orElseThrow();
        assertThat(foundJob.getStatus()).isEqualTo(AiGenerationJobStatus.FAILED);
        assertThat(foundJob.getErrorMessage()).isEqualTo("AUTO_VALIDATION_CONFIGURATION_ERROR");
        assertThat(validationLogRepository.findAll()).isEmpty();
    }

    @Test
    void invalidJsonFailsJobWithoutAsset() {
        AiPromptVersion promptVersion = persistPromptVersion(AiPromptType.EXPLANATION);
        AiGenerationJob job = persistJob(AiGenerationJobType.EXPLANATION, promptVersion);
        givenQtAndBibleContext(List.of(1001L));
        when(llmClient.complete(any())).thenReturn(completionResponse("{"));
        AiGenerationJobRunner runner = runner(explanationHandler(), new SimulatorGenerationJobHandler());

        assertThat(runner.runJob(job.getId())).isTrue();
        flushAndClear();

        AiGenerationJob foundJob = generationJobRepository.findById(job.getId()).orElseThrow();
        assertThat(foundJob.getStatus()).isEqualTo(AiGenerationJobStatus.FAILED);
        assertThat(foundJob.getErrorMessage()).isEqualTo("LLM_RESPONSE_INVALID_JSON");
        assertThat(generatedAssetRepository.findAll()).isEmpty();
        assertThat(validationLogRepository.findAll()).isEmpty();
    }

    @Test
    void outOfScopeVerseIdFailsJobWithoutAsset() {
        AiPromptVersion promptVersion = persistPromptVersion(AiPromptType.EXPLANATION);
        AiGenerationJob job = persistJob(AiGenerationJobType.EXPLANATION, promptVersion);
        givenQtAndBibleContext(List.of(1001L));
        when(llmClient.complete(any())).thenReturn(completionResponse("""
                {
                  "explanations": [
                    {"verseId": 9999, "summary": "summary", "explanation": "explanation"}
                  ],
                  "glossaryTerms": []
                }
                """));
        AiGenerationJobRunner runner = runner(explanationHandler(), new SimulatorGenerationJobHandler());

        assertThat(runner.runJob(job.getId())).isTrue();
        flushAndClear();

        AiGenerationJob foundJob = generationJobRepository.findById(job.getId()).orElseThrow();
        assertThat(foundJob.getStatus()).isEqualTo(AiGenerationJobStatus.FAILED);
        assertThat(foundJob.getErrorMessage()).isEqualTo("LLM_RESPONSE_VERSE_ID_OUT_OF_SCOPE");
        assertThat(generatedAssetRepository.findAll()).isEmpty();
        assertThat(validationLogRepository.findAll()).isEmpty();
    }

    @Test
    void simulatorJobFailsWithDisabledReasonWithoutCallingLlm() {
        AiPromptVersion promptVersion = persistPromptVersion(AiPromptType.SIMULATOR);
        AiGenerationJob job = persistJob(AiGenerationJobType.SIMULATOR, promptVersion);
        AiGenerationJobRunner runner = runner(explanationHandler(), new SimulatorGenerationJobHandler());

        assertThat(runner.runJob(job.getId())).isTrue();
        flushAndClear();

        AiGenerationJob foundJob = generationJobRepository.findById(job.getId()).orElseThrow();
        assertThat(foundJob.getStatus()).isEqualTo(AiGenerationJobStatus.FAILED);
        assertThat(foundJob.getErrorMessage()).isEqualTo("SIMULATOR_GENERATION_DISABLED");
        assertThat(generatedAssetRepository.findAll()).isEmpty();
        assertThat(validationLogRepository.findAll()).isEmpty();
        verify(llmClient, never()).complete(any(LlmCompletionRequest.class));
    }

    private AiGenerationJobRunner runner(AiGenerationJobHandler... handlers) {
        return new AiGenerationJobRunner(
                generationJobRepository,
                generatedAssetRepository,
                autoValidationService(),
                List.of(handlers),
                CLOCK,
                new TransactionTemplate(transactionManager)
        );
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

    private void givenQtAndBibleContext(List<Long> verseIds) {
        when(getQtPassageContentContextUseCase.getContentContext(35L))
                .thenReturn(new QtPassageContentContext(
                        35L,
                        LocalDate.of(2026, 5, 29),
                        "QT title",
                        verseIds,
                        true
                ));
        when(getBibleVerseUseCase.getVerses(verseIds))
                .thenReturn(verseIds.stream()
                        .map(verseId -> new BibleVerseResponse(
                                verseId,
                                "TST",
                                1,
                                verseId.intValue(),
                                "neutral text " + verseId,
                                null
                        ))
                        .toList());
    }

    private AiGenerationJobHandler payloadHandler(String payloadJson) {
        return new AiGenerationJobHandler() {
            @Override
            public AiGenerationJobType jobType() {
                return AiGenerationJobType.EXPLANATION;
            }

            @Override
            public AiGeneratedAsset generate(AiGenerationJob job, OffsetDateTime createdAt) {
                return AiGeneratedAsset.create(
                        job.getId(),
                        AiGeneratedAssetType.EXPLANATION,
                        job.getTargetType(),
                        job.getTargetId(),
                        payloadJson,
                        "QT-AI DeepSeek",
                        createdAt
                );
            }
        };
    }

    private AiPromptVersion persistPromptVersion(AiPromptType promptType) {
        AiPromptVersion promptVersion = new AiPromptVersion();
        setField(promptVersion, "promptType", promptType);
        setField(promptVersion, "version", "2026.05." + promptType);
        setField(promptVersion, "contentHash", "hash-" + promptType);
        setField(promptVersion, "status", AiPromptVersionStatus.ACTIVE);
        setField(promptVersion, "createdAt", BASE_TIME.minusDays(1));
        return testEntityManager.persistAndFlush(promptVersion);
    }

    private AiGenerationJob persistJob(AiGenerationJobType jobType, AiPromptVersion promptVersion) {
        return testEntityManager.persistAndFlush(AiGenerationJob.queue(
                jobType,
                AiTargetType.QT_PASSAGE,
                35L,
                promptVersion.getId(),
                BASE_TIME
        ));
    }

    private AiValidationChecklistVersion persistActiveChecklistVersion() {
        AiValidationChecklistVersion checklistVersion = AiValidationChecklistVersion.create(
                AiValidationChecklistType.EXPLANATION,
                "2026.05.29",
                "sha256:explanation-auto-validation",
                null,
                BASE_TIME.minusDays(1)
        );
        checklistVersion.activate(BASE_TIME.minusHours(1));
        return testEntityManager.persistAndFlush(checklistVersion);
    }

    private void flushAndClear() {
        testEntityManager.flush();
        testEntityManager.clear();
    }

    private static LlmCompletionResponse completionResponse(String content) {
        return new LlmCompletionResponse(
                content,
                11,
                22,
                33,
                "deepseek-test"
        );
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
