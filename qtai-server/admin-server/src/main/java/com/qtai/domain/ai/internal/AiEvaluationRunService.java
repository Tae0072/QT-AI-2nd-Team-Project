package com.qtai.domain.ai.internal;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.client.RestClientException;

import com.qtai.common.exception.BusinessException;
import com.qtai.common.exception.ErrorCode;
import com.qtai.domain.ai.api.admin.evaluation.CreateAiEvaluationRunUseCase;
import com.qtai.domain.ai.api.admin.evaluation.GetAiEvaluationRunUseCase;
import com.qtai.domain.ai.api.admin.evaluation.GetLatestAiEvaluationRunUseCase;
import com.qtai.domain.ai.api.admin.evaluation.dto.AiEvaluationRunResponse;
import com.qtai.domain.ai.api.admin.evaluation.dto.AiEvaluationRunResultResponse;
import com.qtai.domain.ai.api.admin.evaluation.dto.CreateAiEvaluationRunCommand;
import com.qtai.domain.ai.api.admin.evaluation.dto.GetAiEvaluationRunQuery;
import com.qtai.domain.ai.api.admin.evaluation.dto.GetLatestAiEvaluationRunQuery;
import com.qtai.domain.audit.api.WriteAuditLogUseCase;
import com.qtai.domain.audit.api.dto.AuditLogWriteRequest;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class AiEvaluationRunService implements
        CreateAiEvaluationRunUseCase,
        GetLatestAiEvaluationRunUseCase,
        GetAiEvaluationRunUseCase {

    private static final String ACTOR_TYPE_ADMIN = "ADMIN";
    private static final String TARGET_TYPE_RUN = "AI_EVALUATION_RUN";
    private static final String LLM_PROVIDER_ERROR = "LLM_PROVIDER_ERROR";
    private static final String LLM_RATE_LIMIT = "LLM_RATE_LIMIT";
    private static final String LLM_TIMEOUT = "LLM_TIMEOUT";

    private final AiEvaluationSetRepository setRepository;
    private final AiEvaluationCaseRepository caseRepository;
    private final AiPromptVersionRepository promptVersionRepository;
    private final AiEvaluationRunRepository runRepository;
    private final AiEvaluationResultRepository resultRepository;
    private final ExplanationGenerationJobHandler explanationGenerationJobHandler;
    private final WriteAuditLogUseCase auditLogUseCase;
    private final ObjectMapper objectMapper;
    private final Clock clock;
    private final TransactionTemplate transactionTemplate;

    @Autowired
    public AiEvaluationRunService(
            AiEvaluationSetRepository setRepository,
            AiEvaluationCaseRepository caseRepository,
            AiPromptVersionRepository promptVersionRepository,
            AiEvaluationRunRepository runRepository,
            AiEvaluationResultRepository resultRepository,
            ExplanationGenerationJobHandler explanationGenerationJobHandler,
            WriteAuditLogUseCase auditLogUseCase,
            ObjectMapper objectMapper,
            PlatformTransactionManager transactionManager
    ) {
        this(setRepository, caseRepository, promptVersionRepository, runRepository, resultRepository,
                explanationGenerationJobHandler, auditLogUseCase, objectMapper, Clock.systemDefaultZone(),
                transactionManager);
    }

    AiEvaluationRunService(
            AiEvaluationSetRepository setRepository,
            AiEvaluationCaseRepository caseRepository,
            AiPromptVersionRepository promptVersionRepository,
            AiEvaluationRunRepository runRepository,
            AiEvaluationResultRepository resultRepository,
            ExplanationGenerationJobHandler explanationGenerationJobHandler,
            WriteAuditLogUseCase auditLogUseCase,
            ObjectMapper objectMapper,
            Clock clock,
            PlatformTransactionManager transactionManager
    ) {
        this.setRepository = setRepository;
        this.caseRepository = caseRepository;
        this.promptVersionRepository = promptVersionRepository;
        this.runRepository = runRepository;
        this.resultRepository = resultRepository;
        this.explanationGenerationJobHandler = explanationGenerationJobHandler;
        this.auditLogUseCase = auditLogUseCase;
        this.objectMapper = objectMapper;
        this.clock = clock;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public AiEvaluationRunResponse createEvaluationRun(CreateAiEvaluationRunCommand command) {
        requireValidCreateCommand(command);
        requireEvaluationManager(command.memberRole(), command.adminRole());

        EvaluationRunPlan plan = startEvaluationRun(command);

        int passedCount = 0;
        int failedCount = 0;
        List<EvaluationCaseResultDraft> resultDrafts = new ArrayList<>();
        try {
            for (AiEvaluationCase evaluationCase : plan.cases()) {
                EvaluationCaseResultDraft result = executeCase(
                        plan.promptVersion(),
                        evaluationCase,
                        OffsetDateTime.now(clock)
                );
                resultDrafts.add(result);
                if (result.result() == AiEvaluationResultStatus.PASSED) {
                    passedCount++;
                } else if (result.result() == AiEvaluationResultStatus.FAILED) {
                    failedCount++;
                }
            }
            return completeEvaluationRun(command.adminId(), plan.runId(), resultDrafts, passedCount, failedCount);
        } catch (RuntimeException exception) {
            markRunFailed(plan.runId());
            throw exception;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public AiEvaluationRunResponse getLatestEvaluationRun(GetLatestAiEvaluationRunQuery query) {
        requireValidLatestQuery(query);
        requireEvaluationManager(query.memberRole(), query.adminRole());
        AiEvaluationRun run = runRepository.findFirstByEvaluationSetIdOrderByCreatedAtDescIdDesc(query.evaluationSetId())
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
        return toResponse(run, resultRepository.findByEvaluationRunIdOrderByIdAsc(run.getId()));
    }

    @Override
    @Transactional(readOnly = true)
    public AiEvaluationRunResponse getEvaluationRun(GetAiEvaluationRunQuery query) {
        requireValidRunQuery(query);
        requireEvaluationManager(query.memberRole(), query.adminRole());
        AiEvaluationRun run = runRepository.findById(query.runId())
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
        return toResponse(run, resultRepository.findByEvaluationRunIdOrderByIdAsc(run.getId()));
    }

    private EvaluationRunPlan startEvaluationRun(CreateAiEvaluationRunCommand command) {
        return transactionTemplate.execute(status -> {
            AiEvaluationSet set = findSet(command.evaluationSetId());
            requireRunnableExplanationSet(set);
            AiPromptVersion promptVersion = findPromptVersion(command.promptVersionId());
            requireRunnableExplanationPrompt(promptVersion);
            List<AiEvaluationCase> cases = caseRepository.findByEvaluationSetIdAndStatusOrderByIdAsc(
                    set.getId(),
                    AiEvaluationCaseStatus.APPROVED
            );
            if (cases.isEmpty()) {
                throw new BusinessException(ErrorCode.INVALID_STATUS_TRANSITION, "EVALUATION_RUN_REQUIRES_APPROVED_CASES");
            }

            OffsetDateTime now = OffsetDateTime.now(clock);
            AiEvaluationRun run = runRepository.saveAndFlush(AiEvaluationRun.start(
                    set.getId(),
                    promptVersion.getId(),
                    command.adminId(),
                    now
            ));
            return new EvaluationRunPlan(run.getId(), promptVersion, List.copyOf(cases));
        });
    }

    private AiEvaluationRunResponse completeEvaluationRun(
            Long adminId,
            Long runId,
            List<EvaluationCaseResultDraft> resultDrafts,
            int passedCount,
            int failedCount
    ) {
        return transactionTemplate.execute(status -> {
            AiEvaluationRun run = runRepository.findById(runId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
            List<AiEvaluationResult> savedResults = new ArrayList<>();
            for (EvaluationCaseResultDraft draft : resultDrafts) {
                savedResults.add(resultRepository.save(AiEvaluationResult.create(
                        run.getId(),
                        draft.evaluationCaseId(),
                        draft.result(),
                        draft.reason(),
                        draft.outputSummaryJson(),
                        draft.createdAt()
                )));
            }
            run.finish(resultDrafts.size(), passedCount, failedCount, 0, OffsetDateTime.now(clock));
            writeAudit(adminId, run, savedResults);
            return toResponse(run, savedResults);
        });
    }

    private void markRunFailed(Long runId) {
        transactionTemplate.executeWithoutResult(status -> runRepository.findById(runId).ifPresent(run -> {
            if (run.getStatus() == AiEvaluationRunStatus.RUNNING) {
                run.fail(OffsetDateTime.now(clock));
            }
        }));
    }

    int sweepStaleRunningRuns(long timeoutMillis, int batchSize) {
        if (timeoutMillis <= 0 || batchSize < 1) {
            return 0;
        }
        OffsetDateTime threshold = OffsetDateTime.now(clock).minusNanos(timeoutMillis * 1_000_000L);
        List<Long> staleIds = runRepository.findStaleRunningRunIds(threshold, PageRequest.of(0, batchSize));
        int sweptCount = 0;
        for (Long runId : staleIds) {
            boolean swept = Boolean.TRUE.equals(transactionTemplate.execute(status ->
                    runRepository.findByIdAndStatus(runId, AiEvaluationRunStatus.RUNNING)
                            .map(run -> {
                                run.fail(OffsetDateTime.now(clock));
                                return true;
                            })
                            .orElse(false)
            ));
            if (swept) {
                log.warn("Stale AI evaluation RUNNING run marked FAILED. runId={}", runId);
                sweptCount++;
            }
        }
        return sweptCount;
    }

    private EvaluationCaseResultDraft executeCase(
            AiPromptVersion promptVersion,
            AiEvaluationCase evaluationCase,
            OffsetDateTime createdAt
    ) {
        try {
            if (evaluationCase.getTargetType() == AiTargetType.QA_REQUEST) {
                throw new BusinessException(ErrorCode.INVALID_INPUT, "QA_REQUEST_EVALUATION_IS_OUT_OF_SCOPE");
            }
            Long targetId = requirePositive(evaluationCase.getTargetId(), "targetId");
            ExplanationGenerationJobHandler.GeneratedExplanation generated =
                    generateWithSingleRetry(
                    promptVersion,
                    evaluationCase.getTargetType(),
                    targetId
            );
            return new EvaluationCaseResultDraft(
                    evaluationCase.getId(),
                    AiEvaluationResultStatus.PASSED,
                    null,
                    outputSummaryJson(promptVersion, evaluationCase, generated),
                    createdAt
            );
        } catch (BusinessException exception) {
            return failedResult(evaluationCase, safeFailureReason(exception), createdAt);
        }
    }

    private ExplanationGenerationJobHandler.GeneratedExplanation generateWithSingleRetry(
            AiPromptVersion promptVersion,
            AiTargetType targetType,
            Long targetId
    ) {
        try {
            return generateOnce(promptVersion, targetType, targetId);
        } catch (BusinessException exception) {
            if (!isRetryableLlmFailure(exception)) {
                throw exception;
            }
            return generateOnce(promptVersion, targetType, targetId);
        }
    }

    private ExplanationGenerationJobHandler.GeneratedExplanation generateOnce(
            AiPromptVersion promptVersion,
            AiTargetType targetType,
            Long targetId
    ) {
        try {
            return explanationGenerationJobHandler.generateForEvaluation(promptVersion, targetType, targetId);
        } catch (RestClientException exception) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, LLM_PROVIDER_ERROR);
        }
    }

    private EvaluationCaseResultDraft failedResult(
            AiEvaluationCase evaluationCase,
            String reason,
            OffsetDateTime createdAt
    ) {
        return new EvaluationCaseResultDraft(
                evaluationCase.getId(),
                AiEvaluationResultStatus.FAILED,
                reason,
                failureSummaryJson(evaluationCase),
                createdAt
        );
    }

    private static String safeFailureReason(BusinessException exception) {
        String detail = exception.getMessage();
        if (detail != null && detail.matches("[A-Z0-9_]+")) {
            return exception.getErrorCode().name() + ":" + detail;
        }
        return exception.getErrorCode().name();
    }

    private static boolean isRetryableLlmFailure(BusinessException exception) {
        if (exception.getErrorCode() != ErrorCode.INTERNAL_ERROR) {
            return false;
        }
        String detail = exception.getMessage();
        return LLM_TIMEOUT.equals(detail) || LLM_RATE_LIMIT.equals(detail) || LLM_PROVIDER_ERROR.equals(detail);
    }

    private String outputSummaryJson(
            AiPromptVersion promptVersion,
            AiEvaluationCase evaluationCase,
            ExplanationGenerationJobHandler.GeneratedExplanation generated
    ) {
        try {
            JsonNode payload = objectMapper.readTree(generated.payloadJson());
            Map<String, Object> summary = new LinkedHashMap<>();
            summary.put("targetType", evaluationCase.getTargetType().name());
            summary.put("targetId", evaluationCase.getTargetId());
            summary.put("promptVersionId", promptVersion.getId());
            summary.put("promptVersion", promptVersion.getVersion());
            summary.put("promptContentHash", promptVersion.getContentHash());
            summary.put("modelName", generated.modelName());
            summary.put("payloadHash", sha256(generated.payloadJson()));
            summary.put("explanationCount", sizeOf(payload.get("explanations")));
            summary.put("glossaryTermCount", sizeOf(payload.get("glossaryTerms")));
            return objectMapper.writeValueAsString(summary);
        } catch (JsonProcessingException exception) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "evaluation output summary serialization failed");
        }
    }

    private String failureSummaryJson(AiEvaluationCase evaluationCase) {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("targetType", evaluationCase.getTargetType().name());
        summary.put("targetId", evaluationCase.getTargetId());
        try {
            return objectMapper.writeValueAsString(summary);
        } catch (JsonProcessingException exception) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "evaluation failure summary serialization failed");
        }
    }

    private void writeAudit(Long adminId, AiEvaluationRun run, List<AiEvaluationResult> results) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("id", run.getId());
        payload.put("evaluationSetId", run.getEvaluationSetId());
        payload.put("promptVersionId", run.getPromptVersionId());
        payload.put("status", run.getStatus().name());
        payload.put("totalCount", run.getTotalCount());
        payload.put("passedCount", run.getPassedCount());
        payload.put("failedCount", run.getFailedCount());
        payload.put("needsReviewCount", run.getNeedsReviewCount());
        payload.put("resultCount", results.size());
        auditLogUseCase.write(new AuditLogWriteRequest(
                null,
                ACTOR_TYPE_ADMIN,
                adminId,
                ACTOR_TYPE_ADMIN + ":" + adminId,
                "EVAL_RUN_EXECUTE",
                TARGET_TYPE_RUN,
                run.getId(),
                null,
                toJson(payload)
        ));
    }

    private AiEvaluationSet findSet(Long setId) {
        return setRepository.findById(setId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
    }

    private AiPromptVersion findPromptVersion(Long promptVersionId) {
        return promptVersionRepository.findById(promptVersionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
    }

    private static void requireRunnableExplanationSet(AiEvaluationSet set) {
        if (set.getEvalType() != AiEvaluationType.EXPLANATION) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "only EXPLANATION evaluation sets can be executed");
        }
        if (set.getStatus() != AiEvaluationSetStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.INVALID_STATUS_TRANSITION, "evaluation set must be ACTIVE");
        }
        if (set.getTargetType() != AiTargetType.BIBLE_VERSE && set.getTargetType() != AiTargetType.QT_PASSAGE) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "EXPLANATION targetType is not supported");
        }
    }

    private static void requireRunnableExplanationPrompt(AiPromptVersion promptVersion) {
        if (promptVersion.getPromptType() != AiPromptType.EXPLANATION) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "promptVersionId does not match EXPLANATION");
        }
        if (promptVersion.getStatus() == AiPromptVersionStatus.RETIRED) {
            throw new BusinessException(ErrorCode.INVALID_STATUS_TRANSITION, "retired prompt version cannot be evaluated");
        }
    }

    private static AiEvaluationRunResponse toResponse(
            AiEvaluationRun run,
            List<AiEvaluationResult> results
    ) {
        return new AiEvaluationRunResponse(
                run.getId(),
                run.getEvaluationSetId(),
                run.getPromptVersionId(),
                run.getStatus().name(),
                run.getTotalCount(),
                run.getPassedCount(),
                run.getFailedCount(),
                run.getNeedsReviewCount(),
                run.getStartedAt(),
                run.getFinishedAt(),
                run.getRequestedByAdminId(),
                results.stream().map(AiEvaluationRunService::toResultResponse).toList()
        );
    }

    private static AiEvaluationRunResultResponse toResultResponse(AiEvaluationResult result) {
        return new AiEvaluationRunResultResponse(
                result.getId(),
                result.getEvaluationCaseId(),
                result.getResult().name(),
                result.getReason(),
                result.getOutputSummaryJson(),
                result.getCreatedAt()
        );
    }

    private String toJson(Map<String, Object> payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException exception) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "audit snapshot serialization failed");
        }
    }

    private static int sizeOf(JsonNode node) {
        if (node == null || !node.isArray()) {
            return 0;
        }
        return node.size();
    }

    private static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "SHA-256 is not available");
        }
    }

    private static void requireValidCreateCommand(CreateAiEvaluationRunCommand command) {
        if (command == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "command must not be null");
        }
        requirePositive(command.adminId(), "adminId");
        requireText(command.memberRole(), "memberRole");
        requireText(command.adminRole(), "adminRole");
        requirePositive(command.evaluationSetId(), "evaluationSetId");
        requirePositive(command.promptVersionId(), "promptVersionId");
    }

    private static void requireValidLatestQuery(GetLatestAiEvaluationRunQuery query) {
        if (query == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "query must not be null");
        }
        requirePositive(query.adminId(), "adminId");
        requireText(query.memberRole(), "memberRole");
        requireText(query.adminRole(), "adminRole");
        requirePositive(query.evaluationSetId(), "evaluationSetId");
    }

    private static void requireValidRunQuery(GetAiEvaluationRunQuery query) {
        if (query == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "query must not be null");
        }
        requirePositive(query.adminId(), "adminId");
        requireText(query.memberRole(), "memberRole");
        requireText(query.adminRole(), "adminRole");
        requirePositive(query.runId(), "runId");
    }

    private static void requireEvaluationManager(String memberRole, String adminRole) {
        if (!"ADMIN".equals(memberRole)
                || !("REVIEWER".equals(adminRole)
                || "CONTENT_CREATOR".equals(adminRole)
                || "SUPER_ADMIN".equals(adminRole))) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
    }

    private static Long requirePositive(Long value, String fieldName) {
        if (value == null || value <= 0) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, fieldName + " must be positive");
        }
        return value;
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, fieldName + " must not be blank");
        }
        return value;
    }

    private record EvaluationRunPlan(
            Long runId,
            AiPromptVersion promptVersion,
            List<AiEvaluationCase> cases
    ) {
    }

    private record EvaluationCaseResultDraft(
            Long evaluationCaseId,
            AiEvaluationResultStatus result,
            String reason,
            String outputSummaryJson,
            OffsetDateTime createdAt
    ) {
    }
}
