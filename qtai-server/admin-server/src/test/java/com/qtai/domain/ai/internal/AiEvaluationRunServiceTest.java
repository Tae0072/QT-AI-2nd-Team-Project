package com.qtai.domain.ai.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;

import com.qtai.common.exception.BusinessException;
import com.qtai.common.exception.ErrorCode;
import com.qtai.domain.ai.api.admin.evaluation.dto.AiEvaluationRunResponse;
import com.qtai.domain.ai.api.admin.evaluation.dto.CreateAiEvaluationRunCommand;
import com.qtai.domain.audit.api.WriteAuditLogUseCase;

@ExtendWith(MockitoExtension.class)
class AiEvaluationRunServiceTest {

    private static final OffsetDateTime NOW = OffsetDateTime.parse("2026-06-15T10:00:00+09:00");

    @Mock
    private AiEvaluationSetRepository setRepository;
    @Mock
    private AiEvaluationCaseRepository caseRepository;
    @Mock
    private AiPromptVersionRepository promptVersionRepository;
    @Mock
    private AiEvaluationRunRepository runRepository;
    @Mock
    private AiEvaluationResultRepository resultRepository;
    @Mock
    private ExplanationGenerationJobHandler explanationGenerationJobHandler;
    @Mock
    private WriteAuditLogUseCase auditLogUseCase;

    private RecordingTransactionManager transactionManager;
    private AiEvaluationRunService service;

    @BeforeEach
    void setUp() {
        transactionManager = new RecordingTransactionManager();
        service = new AiEvaluationRunService(
                setRepository,
                caseRepository,
                promptVersionRepository,
                runRepository,
                resultRepository,
                explanationGenerationJobHandler,
                auditLogUseCase,
                new ObjectMapper(),
                Clock.fixed(Instant.parse("2026-06-15T01:00:00Z"), ZoneId.of("Asia/Seoul")),
                transactionManager
        );
    }

    @Test
    void createEvaluationRunStoresOnlyOutputSummary() throws Exception {
        AiEvaluationSet set = AiEvaluationSet.create(
                "EXPLANATION set",
                AiEvaluationType.EXPLANATION,
                "2026.06.1",
                AiTargetType.BIBLE_VERSE,
                null,
                null,
                NOW.minusDays(1)
        );
        setId(set, 10L);
        set.activate(NOW.minusHours(1));

        AiEvaluationCase evaluationCase = AiEvaluationCase.create(
                10L,
                AiTargetType.BIBLE_VERSE,
                1001L,
                AiEvaluationSourceType.ADMIN_CREATED,
                null,
                "{\"targetType\":\"BIBLE_VERSE\",\"targetId\":1001}",
                null,
                null,
                NOW.minusDays(1)
        );
        setId(evaluationCase, 20L);
        evaluationCase.approve(99L, NOW.minusHours(2));

        AiPromptVersion promptVersion = AiPromptVersion.of(
                2L,
                AiPromptType.EXPLANATION,
                "2026.06.2",
                "hash-002",
                AiPromptVersionStatus.DRAFT,
                AiPromptVersion.defaultSystemPrompt(),
                AiPromptVersion.defaultUserPromptTemplate(),
                null,
                0.2,
                2000,
                null,
                99L,
                NOW.minusDays(1),
                null,
                null
        );

        AtomicReference<AiEvaluationResult> savedResult = new AtomicReference<>();
        AtomicReference<AiEvaluationRun> savedRun = new AtomicReference<>();
        AtomicLong resultId = new AtomicLong(300L);
        when(setRepository.findById(10L)).thenReturn(Optional.of(set));
        when(promptVersionRepository.findById(2L)).thenReturn(Optional.of(promptVersion));
        when(caseRepository.findByEvaluationSetIdAndStatusOrderByIdAsc(10L, AiEvaluationCaseStatus.APPROVED))
                .thenReturn(List.of(evaluationCase));
        when(runRepository.saveAndFlush(any(AiEvaluationRun.class))).thenAnswer(invocation -> {
            AiEvaluationRun run = invocation.getArgument(0);
            setId(run, 200L);
            savedRun.set(run);
            return run;
        });
        when(runRepository.findById(200L)).thenAnswer(invocation -> Optional.of(savedRun.get()));
        when(explanationGenerationJobHandler.generateForEvaluation(promptVersion, AiTargetType.BIBLE_VERSE, 1001L))
                .thenThrow(new BusinessException(ErrorCode.INTERNAL_ERROR, "LLM_TIMEOUT"))
                .thenAnswer(invocation -> {
                    assertThat(transactionManager.isTransactionActive()).isFalse();
                    return new ExplanationGenerationJobHandler.GeneratedExplanation(
                        """
                                {
                                  "explanations":[{"verseId":1001,"summary":"raw summary","explanation":"raw body"}],
                                  "glossaryTerms":[{"verseId":1001,"term":"raw term","meaning":"raw meaning"}]
                                }
                                """,
                        "deepseek-chat"
                    );
                });
        when(resultRepository.save(any(AiEvaluationResult.class))).thenAnswer(invocation -> {
            AiEvaluationResult result = invocation.getArgument(0);
            setId(result, resultId.getAndIncrement());
            savedResult.set(result);
            return result;
        });
        AiEvaluationRunResponse response = service.createEvaluationRun(new CreateAiEvaluationRunCommand(
                99L,
                "ADMIN",
                "REVIEWER",
                10L,
                2L
        ));

        assertThat(response.status()).isEqualTo("SUCCEEDED");
        assertThat(response.totalCount()).isEqualTo(1);
        assertThat(response.passedCount()).isEqualTo(1);
        assertThat(response.failedCount()).isZero();
        assertThat(response.results()).hasSize(1);
        assertThat(response.results().get(0).outputSummaryJson())
                .contains("\"payloadHash\"")
                .contains("\"explanationCount\":1")
                .doesNotContain("raw body")
                .doesNotContain("raw meaning");
        verify(explanationGenerationJobHandler, times(2))
                .generateForEvaluation(promptVersion, AiTargetType.BIBLE_VERSE, 1001L);
    }

    @Test
    void createEvaluationRunStoresSafeFailureReasonOnly() throws Exception {
        AiEvaluationSet set = AiEvaluationSet.create(
                "EXPLANATION set",
                AiEvaluationType.EXPLANATION,
                "2026.06.1",
                AiTargetType.BIBLE_VERSE,
                null,
                null,
                NOW.minusDays(1)
        );
        setId(set, 10L);
        set.activate(NOW.minusHours(1));

        AiEvaluationCase evaluationCase = AiEvaluationCase.create(
                10L,
                AiTargetType.BIBLE_VERSE,
                1001L,
                AiEvaluationSourceType.ADMIN_CREATED,
                null,
                "{\"targetType\":\"BIBLE_VERSE\",\"targetId\":1001}",
                null,
                null,
                NOW.minusDays(1)
        );
        setId(evaluationCase, 20L);
        evaluationCase.approve(99L, NOW.minusHours(2));

        AiPromptVersion promptVersion = AiPromptVersion.of(
                2L,
                AiPromptType.EXPLANATION,
                "2026.06.2",
                "hash-002",
                AiPromptVersionStatus.DRAFT,
                AiPromptVersion.defaultSystemPrompt(),
                AiPromptVersion.defaultUserPromptTemplate(),
                null,
                0.2,
                2000,
                null,
                99L,
                NOW.minusDays(1),
                null,
                null
        );

        AtomicReference<AiEvaluationResult> savedResult = new AtomicReference<>();
        AtomicReference<AiEvaluationRun> savedRun = new AtomicReference<>();
        when(setRepository.findById(10L)).thenReturn(Optional.of(set));
        when(promptVersionRepository.findById(2L)).thenReturn(Optional.of(promptVersion));
        when(caseRepository.findByEvaluationSetIdAndStatusOrderByIdAsc(10L, AiEvaluationCaseStatus.APPROVED))
                .thenReturn(List.of(evaluationCase));
        when(runRepository.saveAndFlush(any(AiEvaluationRun.class))).thenAnswer(invocation -> {
            AiEvaluationRun run = invocation.getArgument(0);
            setId(run, 200L);
            savedRun.set(run);
            return run;
        });
        when(runRepository.findById(200L)).thenAnswer(invocation -> Optional.of(savedRun.get()));
        when(explanationGenerationJobHandler.generateForEvaluation(promptVersion, AiTargetType.BIBLE_VERSE, 1001L))
                .thenThrow(new BusinessException(ErrorCode.INTERNAL_ERROR, "raw provider response body"));
        when(resultRepository.save(any(AiEvaluationResult.class))).thenAnswer(invocation -> {
            AiEvaluationResult result = invocation.getArgument(0);
            setId(result, 300L);
            savedResult.set(result);
            return result;
        });

        AiEvaluationRunResponse response = service.createEvaluationRun(new CreateAiEvaluationRunCommand(
                99L,
                "ADMIN",
                "REVIEWER",
                10L,
                2L
        ));

        assertThat(response.status()).isEqualTo("SUCCEEDED");
        assertThat(response.failedCount()).isEqualTo(1);
        assertThat(savedResult.get().getReason())
                .isEqualTo("INTERNAL_ERROR")
                .doesNotContain("raw provider response body");
    }

    @Test
    void sweepStaleRunningRunsMarksTimedOutRunFailed() throws Exception {
        AiEvaluationRun run = AiEvaluationRun.start(10L, 2L, 99L, NOW.minusMinutes(10));
        setId(run, 200L);
        when(runRepository.findStaleRunningRunIds(any(), any())).thenReturn(List.of(200L));
        when(runRepository.findByIdAndStatus(200L, AiEvaluationRunStatus.RUNNING)).thenReturn(Optional.of(run));

        int sweptCount = service.sweepStaleRunningRuns(300_000L, 10);

        assertThat(sweptCount).isEqualTo(1);
        assertThat(run.getStatus()).isEqualTo(AiEvaluationRunStatus.FAILED);
        assertThat(run.getFinishedAt()).isEqualTo(NOW);
    }

    private static void setId(Object target, Long id) throws ReflectiveOperationException {
        Field idField = target.getClass().getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(target, id);
    }

    private static final class RecordingTransactionManager extends AbstractPlatformTransactionManager {

        private boolean transactionActive;

        boolean isTransactionActive() {
            return transactionActive;
        }

        @Override
        protected Object doGetTransaction() {
            return new Object();
        }

        @Override
        protected void doBegin(Object transaction, TransactionDefinition definition) {
            transactionActive = true;
        }

        @Override
        protected void doCommit(DefaultTransactionStatus status) {
            transactionActive = false;
        }

        @Override
        protected void doRollback(DefaultTransactionStatus status) {
            transactionActive = false;
        }
    }
}
