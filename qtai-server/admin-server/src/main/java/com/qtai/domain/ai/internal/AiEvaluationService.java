package com.qtai.domain.ai.internal;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.qtai.common.exception.BusinessException;
import com.qtai.common.exception.ErrorCode;
import com.qtai.domain.ai.api.admin.evaluation.ActivateAiEvaluationSetUseCase;
import com.qtai.domain.ai.api.admin.evaluation.ApproveAiEvaluationCaseUseCase;
import com.qtai.domain.ai.api.admin.evaluation.CreateAiEvaluationAssetCandidateUseCase;
import com.qtai.domain.ai.api.admin.evaluation.CreateAiEvaluationCaseUseCase;
import com.qtai.domain.ai.api.admin.evaluation.CreateAiEvaluationReportCandidateUseCase;
import com.qtai.domain.ai.api.admin.evaluation.CreateAiEvaluationSetUseCase;
import com.qtai.domain.ai.api.admin.evaluation.GetAiEvaluationCaseUseCase;
import com.qtai.domain.ai.api.admin.evaluation.GetAiEvaluationSetUseCase;
import com.qtai.domain.ai.api.admin.evaluation.ListAiEvaluationCasesUseCase;
import com.qtai.domain.ai.api.admin.evaluation.ListAiEvaluationSetsUseCase;
import com.qtai.domain.ai.api.admin.evaluation.RejectAiEvaluationCaseUseCase;
import com.qtai.domain.ai.api.admin.evaluation.RetireAiEvaluationSetUseCase;
import com.qtai.domain.ai.api.admin.evaluation.dto.AiEvaluationCaseListResponse;
import com.qtai.domain.ai.api.admin.evaluation.dto.AiEvaluationCaseResponse;
import com.qtai.domain.ai.api.admin.evaluation.dto.AiEvaluationCaseStatusResponse;
import com.qtai.domain.ai.api.admin.evaluation.dto.AiEvaluationSetListResponse;
import com.qtai.domain.ai.api.admin.evaluation.dto.AiEvaluationSetResponse;
import com.qtai.domain.ai.api.admin.evaluation.dto.ChangeAiEvaluationCaseStatusCommand;
import com.qtai.domain.ai.api.admin.evaluation.dto.ChangeAiEvaluationSetStatusCommand;
import com.qtai.domain.ai.api.admin.evaluation.dto.CreateAiEvaluationAssetCandidateCommand;
import com.qtai.domain.ai.api.admin.evaluation.dto.CreateAiEvaluationCaseCommand;
import com.qtai.domain.ai.api.admin.evaluation.dto.CreateAiEvaluationReportCandidateCommand;
import com.qtai.domain.ai.api.admin.evaluation.dto.CreateAiEvaluationSetCommand;
import com.qtai.domain.ai.api.admin.evaluation.dto.GetAiEvaluationCaseQuery;
import com.qtai.domain.ai.api.admin.evaluation.dto.GetAiEvaluationSetQuery;
import com.qtai.domain.ai.api.admin.evaluation.dto.ListAiEvaluationCasesQuery;
import com.qtai.domain.ai.api.admin.evaluation.dto.ListAiEvaluationSetsQuery;
import com.qtai.domain.audit.api.WriteAuditLogUseCase;
import com.qtai.domain.audit.api.dto.AuditLogWriteRequest;
import com.qtai.domain.report.api.GetReportUseCase;
import com.qtai.domain.report.api.dto.ReportForEvaluation;

@Service
public class AiEvaluationService implements
        ListAiEvaluationSetsUseCase,
        CreateAiEvaluationSetUseCase,
        GetAiEvaluationSetUseCase,
        ActivateAiEvaluationSetUseCase,
        RetireAiEvaluationSetUseCase,
        ListAiEvaluationCasesUseCase,
        CreateAiEvaluationCaseUseCase,
        GetAiEvaluationCaseUseCase,
        ApproveAiEvaluationCaseUseCase,
        RejectAiEvaluationCaseUseCase,
        CreateAiEvaluationAssetCandidateUseCase,
        CreateAiEvaluationReportCandidateUseCase {

    private static final int MAX_PAGE_SIZE = 100;
    private static final int MIN_APPROVED_CASES_TO_ACTIVATE = 10;
    private static final String SORT = "createdAt,desc,id,desc";
    private static final String ACTOR_TYPE_ADMIN = "ADMIN";
    private static final String TARGET_TYPE_CASE = "AI_EVALUATION_CASE";

    private final AiEvaluationSetRepository setRepository;
    private final AiEvaluationCaseRepository caseRepository;
    private final AiGeneratedAssetRepository assetRepository;
    private final WriteAuditLogUseCase auditLogUseCase;
    private final GetReportUseCase getReportUseCase;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    @Autowired
    public AiEvaluationService(
            AiEvaluationSetRepository setRepository,
            AiEvaluationCaseRepository caseRepository,
            AiGeneratedAssetRepository assetRepository,
            WriteAuditLogUseCase auditLogUseCase,
            GetReportUseCase getReportUseCase,
            ObjectMapper objectMapper
    ) {
        this(setRepository, caseRepository, assetRepository, auditLogUseCase, getReportUseCase, objectMapper,
                Clock.systemDefaultZone());
    }

    AiEvaluationService(
            AiEvaluationSetRepository setRepository,
            AiEvaluationCaseRepository caseRepository,
            AiGeneratedAssetRepository assetRepository,
            WriteAuditLogUseCase auditLogUseCase,
            GetReportUseCase getReportUseCase,
            ObjectMapper objectMapper,
            Clock clock
    ) {
        this.setRepository = setRepository;
        this.caseRepository = caseRepository;
        this.assetRepository = assetRepository;
        this.auditLogUseCase = auditLogUseCase;
        this.getReportUseCase = getReportUseCase;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public AiEvaluationSetListResponse listEvaluationSets(ListAiEvaluationSetsQuery query) {
        requireValidPageQuery(query.adminId(), query.memberRole(), query.adminRole(), query.page(), query.size());
        AiEvaluationType evalType = parseEnum(AiEvaluationType.class, query.evalType(), "evalType");
        AiTargetType targetType = parseEnum(AiTargetType.class, query.targetType(), "targetType");
        AiEvaluationSetStatus status = parseEnum(AiEvaluationSetStatus.class, query.status(), "status");
        PageRequest pageRequest = pageRequest(query.page(), query.size());
        Page<AiEvaluationSet> page = findSetPage(evalType, targetType, status, pageRequest);
        return new AiEvaluationSetListResponse(
                page.getContent().stream().map(AiEvaluationService::toSetResponse).toList(),
                query.page(),
                query.size(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast(),
                SORT
        );
    }

    @Override
    @Transactional
    public AiEvaluationSetResponse createEvaluationSet(CreateAiEvaluationSetCommand command) {
        requireValidCreateSet(command);
        AiEvaluationType evalType = parseEnumRequired(AiEvaluationType.class, command.evalType(), "evalType");
        AiTargetType targetType = parseEnumRequired(AiTargetType.class, command.targetType(), "targetType");
        requireDraftStatus(command.status());
        String expectedPolicyJson = normalizeOptionalJson(command.expectedPolicyJson(), "expectedPolicyJson");
        if (setRepository.existsByEvalTypeAndVersion(evalType, command.version())) {
            throw new BusinessException(ErrorCode.DUPLICATE_RESOURCE);
        }
        AiEvaluationSet set;
        try {
            set = setRepository.save(AiEvaluationSet.create(
                    command.name(),
                    evalType,
                    command.version(),
                    targetType,
                    expectedPolicyJson,
                    command.description(),
                    OffsetDateTime.now(clock)
            ));
        } catch (DataIntegrityViolationException exception) {
            throw new BusinessException(ErrorCode.DUPLICATE_RESOURCE);
        }
        return toSetResponse(set);
    }

    @Override
    @Transactional(readOnly = true)
    public AiEvaluationSetResponse getEvaluationSet(GetAiEvaluationSetQuery query) {
        requireValidGetSet(query);
        return toSetResponse(findSet(query.setId()));
    }

    @Override
    @Transactional
    public AiEvaluationSetResponse activateEvaluationSet(ChangeAiEvaluationSetStatusCommand command) {
        requireValidSetStatusCommand(command);
        AiEvaluationSet set = findSet(command.setId());
        long approvedCases = caseRepository.countByEvaluationSetIdAndStatus(set.getId(), AiEvaluationCaseStatus.APPROVED);
        if (approvedCases < MIN_APPROVED_CASES_TO_ACTIVATE) {
            throw new BusinessException(ErrorCode.INVALID_STATUS_TRANSITION);
        }
        set.activate(OffsetDateTime.now(clock));
        return toSetResponse(set);
    }

    @Override
    @Transactional
    public AiEvaluationSetResponse retireEvaluationSet(ChangeAiEvaluationSetStatusCommand command) {
        requireValidSetStatusCommand(command);
        AiEvaluationSet set = findSet(command.setId());
        set.retire(OffsetDateTime.now(clock));
        return toSetResponse(set);
    }

    @Override
    @Transactional(readOnly = true)
    public AiEvaluationCaseListResponse listEvaluationCases(ListAiEvaluationCasesQuery query) {
        requireValidListCases(query);
        AiEvaluationCaseStatus status = parseEnum(AiEvaluationCaseStatus.class, query.status(), "status");
        Page<AiEvaluationCase> page = status == null
                ? caseRepository.findByEvaluationSetId(query.evaluationSetId(), pageRequest(query.page(), query.size()))
                : caseRepository.findByEvaluationSetIdAndStatus(query.evaluationSetId(), status, pageRequest(query.page(), query.size()));
        return new AiEvaluationCaseListResponse(
                page.getContent().stream().map(AiEvaluationService::toCaseResponse).toList(),
                query.page(),
                query.size(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast(),
                SORT
        );
    }

    @Override
    @Transactional
    public AiEvaluationCaseResponse createEvaluationCase(CreateAiEvaluationCaseCommand command) {
        requireValidCreateCase(command);
        findSet(command.evaluationSetId());
        AiTargetType targetType = parseEnumRequired(AiTargetType.class, command.targetType(), "targetType");
        AiEvaluationSourceType sourceType = parseEnumRequired(AiEvaluationSourceType.class, command.sourceType(), "sourceType");
        // 수동 추가도 식별자·메타만 저장한다 — 자유 텍스트(원문/프롬프트/민감정보)는 받지도 저장하지도 않는다(§7).
        String inputJson = toJson(manualCaseSnapshot(targetType, command.targetId(), sourceType));
        AiEvaluationCase saved = caseRepository.save(AiEvaluationCase.create(
                command.evaluationSetId(),
                targetType,
                command.targetId(),
                sourceType,
                null,
                inputJson,
                null,
                normalizeOptionalJson(command.expectedPolicyJson(), "expectedPolicyJson"),
                OffsetDateTime.now(clock)
        ));
        return toCaseResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public AiEvaluationCaseResponse getEvaluationCase(GetAiEvaluationCaseQuery query) {
        requireValidGetCase(query);
        return toCaseResponse(findCase(query.caseId()));
    }

    @Override
    @Transactional
    public AiEvaluationCaseStatusResponse approveEvaluationCase(ChangeAiEvaluationCaseStatusCommand command) {
        return reviewCase(command, true);
    }

    @Override
    @Transactional
    public AiEvaluationCaseStatusResponse rejectEvaluationCase(ChangeAiEvaluationCaseStatusCommand command) {
        return reviewCase(command, false);
    }

    @Override
    @Transactional
    public AiEvaluationCaseResponse createAssetCandidate(CreateAiEvaluationAssetCandidateCommand command) {
        requireValidAssetCandidate(command);
        AiEvaluationSet set = findSet(command.evaluationSetId());
        AiGeneratedAsset asset = assetRepository.findById(command.assetId())
                .orElseThrow(() -> new BusinessException(ErrorCode.AI_ASSET_NOT_FOUND));
        if (asset.getTargetType() != set.getTargetType()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "asset targetType does not match evaluation set");
        }
        String inputJson = toJson(assetCandidateSnapshot(asset));
        AiEvaluationCase saved = caseRepository.save(AiEvaluationCase.create(
                set.getId(),
                asset.getTargetType(),
                asset.getTargetId(),
                AiEvaluationSourceType.VALIDATION_FAILURE,
                asset.getId(),
                inputJson,
                null,
                normalizeOptionalJson(command.expectedPolicyJson(), "expectedPolicyJson"),
                OffsetDateTime.now(clock)
        ));
        return toCaseResponse(saved);
    }

    @Override
    @Transactional
    public AiEvaluationCaseResponse createReportCandidate(CreateAiEvaluationReportCandidateCommand command) {
        requireValidReportCandidate(command);
        AiEvaluationSet set = findSet(command.evaluationSetId());
        ReportForEvaluation report = getReportUseCase.getReportForEvaluation(command.reportId());

        // 신고 대상 유형 → 평가 대상 유형 파생(AI 신고만 허용). FE는 판단값만 보내고 백엔드가 메타로 inputJson 조립.
        final AiTargetType derivedTargetType;
        final Long derivedTargetId;
        final Map<String, Object> snapshot;
        String reportTargetType = report.targetType();
        if ("AI_QA_REQUEST".equals(reportTargetType)) {
            derivedTargetType = AiTargetType.QA_REQUEST;
            derivedTargetId = report.targetId();
            snapshot = reportCandidateSnapshot(report, null);
        } else if ("AI_ASSET".equals(reportTargetType)) {
            AiGeneratedAsset asset = assetRepository.findById(report.targetId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.AI_ASSET_NOT_FOUND));
            derivedTargetType = asset.getTargetType();
            derivedTargetId = asset.getTargetId();
            snapshot = reportCandidateSnapshot(report, asset);
        } else {
            throw new BusinessException(ErrorCode.INVALID_INPUT,
                    "only AI reports (AI_QA_REQUEST/AI_ASSET) can be registered as evaluation candidates");
        }

        if (derivedTargetType != set.getTargetType()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "report target type does not match evaluation set");
        }

        AiEvaluationCase saved = caseRepository.save(AiEvaluationCase.create(
                set.getId(),
                derivedTargetType,
                derivedTargetId,
                AiEvaluationSourceType.USER_REPORT,
                report.id(),
                toJson(snapshot),
                null,
                normalizeOptionalJson(command.expectedPolicyJson(), "expectedPolicyJson"),
                OffsetDateTime.now(clock)
        ));
        return toCaseResponse(saved);
    }

    private AiEvaluationCaseStatusResponse reviewCase(ChangeAiEvaluationCaseStatusCommand command, boolean approve) {
        requireValidCaseStatusCommand(command);
        AiEvaluationCase evaluationCase = findCase(command.caseId());
        String beforeJson = caseAuditSnapshot(evaluationCase, null);
        if (approve) {
            evaluationCase.approve(command.adminId(), command.reviewedAt());
        } else {
            evaluationCase.reject(command.adminId(), command.reviewedAt());
        }
        writeCaseAudit(
                command.adminId(),
                approve ? "EVAL_CASE_APPROVE" : "EVAL_CASE_REJECT",
                evaluationCase.getId(),
                beforeJson,
                caseAuditSnapshot(evaluationCase, command.reviewReason())
        );
        return new AiEvaluationCaseStatusResponse(evaluationCase.getId(), evaluationCase.getStatus().name());
    }

    private Page<AiEvaluationSet> findSetPage(
            AiEvaluationType evalType,
            AiTargetType targetType,
            AiEvaluationSetStatus status,
            PageRequest pageRequest
    ) {
        if (evalType != null && targetType != null && status != null) {
            return setRepository.findByEvalTypeAndTargetTypeAndStatus(evalType, targetType, status, pageRequest);
        }
        if (evalType != null && targetType != null) {
            return setRepository.findByEvalTypeAndTargetType(evalType, targetType, pageRequest);
        }
        if (evalType != null && status != null) {
            return setRepository.findByEvalTypeAndStatus(evalType, status, pageRequest);
        }
        if (targetType != null && status != null) {
            return setRepository.findByTargetTypeAndStatus(targetType, status, pageRequest);
        }
        if (evalType != null && targetType == null && status == null) {
            return setRepository.findByEvalType(evalType, pageRequest);
        }
        if (evalType == null && targetType != null && status == null) {
            return setRepository.findByTargetType(targetType, pageRequest);
        }
        if (evalType == null && targetType == null && status != null) {
            return setRepository.findByStatus(status, pageRequest);
        }
        return setRepository.findAll(pageRequest);
    }

    private AiEvaluationSet findSet(Long setId) {
        return setRepository.findById(setId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
    }

    private AiEvaluationCase findCase(Long caseId) {
        return caseRepository.findById(caseId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
    }

    private static AiEvaluationSetResponse toSetResponse(AiEvaluationSet set) {
        return new AiEvaluationSetResponse(
                set.getId(),
                set.getName(),
                set.getEvalType().name(),
                set.getVersion(),
                set.getTargetType().name(),
                set.getExpectedPolicyJson(),
                set.getDescription(),
                set.getStatus().name(),
                set.getCreatedAt(),
                set.getActivatedAt(),
                set.getRetiredAt()
        );
    }

    private static AiEvaluationCaseResponse toCaseResponse(AiEvaluationCase evaluationCase) {
        return new AiEvaluationCaseResponse(
                evaluationCase.getId(),
                evaluationCase.getEvaluationSetId(),
                evaluationCase.getTargetType().name(),
                evaluationCase.getTargetId(),
                evaluationCase.getSourceType().name(),
                evaluationCase.getSourceId(),
                evaluationCase.getInputJson(),
                evaluationCase.getExpectedOutputJson(),
                evaluationCase.getExpectedPolicyJson(),
                evaluationCase.getStatus().name(),
                evaluationCase.getReviewedByAdminId(),
                evaluationCase.getReviewedAt(),
                evaluationCase.getCreatedAt()
        );
    }

    private Map<String, Object> assetCandidateSnapshot(AiGeneratedAsset asset) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("assetId", asset.getId());
        payload.put("assetType", asset.getAssetType().name());
        payload.put("assetStatus", asset.getStatus().name());
        payload.put("targetType", asset.getTargetType().name());
        payload.put("targetId", asset.getTargetId());
        payload.put("sourceLabel", asset.getSourceLabel());
        return payload;
    }

    /**
     * 신고 후보 inputJson — 식별자·메타데이터만 담는다. 신고 원문/상세(detail)·프롬프트·민감정보는 저장하지 않는다(§7).
     * AI_ASSET 신고면 연결 산출물 메타를 보강한다.
     */
    private Map<String, Object> reportCandidateSnapshot(ReportForEvaluation report, AiGeneratedAsset asset) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("reportId", report.id());
        payload.put("reportTargetType", report.targetType());
        payload.put("reportTargetId", report.targetId());
        payload.put("reason", report.reason());
        payload.put("reportStatus", report.status());
        payload.put("reporterMemberId", report.reporterMemberId());
        if (asset != null) {
            payload.put("linkedAssetId", asset.getId());
            payload.put("linkedAssetType", asset.getAssetType().name());
            payload.put("linkedAssetTargetType", asset.getTargetType().name());
            payload.put("linkedAssetTargetId", asset.getTargetId());
            payload.put("linkedAssetSourceLabel", asset.getSourceLabel());
        }
        return payload;
    }

    /** 수동 추가 inputJson — 식별자·메타만 담는다(자유 텍스트/원문/프롬프트 미저장, §7). */
    private Map<String, Object> manualCaseSnapshot(AiTargetType targetType, Long targetId, AiEvaluationSourceType sourceType) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("targetType", targetType.name());
        payload.put("targetId", targetId);
        payload.put("sourceType", sourceType.name());
        return payload;
    }

    private String caseAuditSnapshot(AiEvaluationCase evaluationCase, String reviewReason) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("id", evaluationCase.getId());
        payload.put("evaluationSetId", evaluationCase.getEvaluationSetId());
        payload.put("targetType", evaluationCase.getTargetType().name());
        payload.put("targetId", evaluationCase.getTargetId());
        payload.put("sourceType", evaluationCase.getSourceType().name());
        payload.put("status", evaluationCase.getStatus().name());
        payload.put("reviewedAt", evaluationCase.getReviewedAt());
        if (reviewReason != null && !reviewReason.isBlank()) {
            payload.put("reviewReason", reviewReason);
        }
        return toJson(payload);
    }

    private void writeCaseAudit(Long adminId, String actionType, Long targetId, String beforeJson, String afterJson) {
        auditLogUseCase.write(new AuditLogWriteRequest(
                null,
                ACTOR_TYPE_ADMIN,
                adminId,
                "ADMIN:" + adminId,
                actionType,
                TARGET_TYPE_CASE,
                targetId,
                beforeJson,
                afterJson
        ));
    }

    private String normalizeOptionalJson(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(objectMapper.readTree(value));
        } catch (JsonProcessingException exception) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, fieldName + " must be valid JSON");
        }
    }

    private String toJson(Map<String, Object> payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException exception) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "JSON serialization failed");
        }
    }

    private static void requireDraftStatus(String status) {
        if (status == null || status.isBlank() || AiEvaluationSetStatus.DRAFT.name().equals(status)) {
            return;
        }
        throw new BusinessException(ErrorCode.INVALID_INPUT, "status must be DRAFT when provided");
    }

    private static void requireValidCreateSet(CreateAiEvaluationSetCommand command) {
        if (command == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
        requireValidActor(command.adminId(), command.memberRole(), command.adminRole());
        requireText(command.name(), "name");
        requireText(command.evalType(), "evalType");
        requireText(command.version(), "version");
        requireText(command.targetType(), "targetType");
    }

    private static void requireValidCreateCase(CreateAiEvaluationCaseCommand command) {
        if (command == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
        requireValidActor(command.adminId(), command.memberRole(), command.adminRole());
        requirePositive(command.evaluationSetId(), "evaluationSetId");
        requireText(command.targetType(), "targetType");
        requirePositive(command.targetId(), "targetId");
        requireText(command.sourceType(), "sourceType");
        if (command.status() != null && !command.status().isBlank()
                && !AiEvaluationCaseStatus.CANDIDATE.name().equals(command.status())) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "status must be CANDIDATE when provided");
        }
    }

    private static void requireValidAssetCandidate(CreateAiEvaluationAssetCandidateCommand command) {
        if (command == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
        requireValidActor(command.adminId(), command.memberRole(), command.adminRole());
        requirePositive(command.evaluationSetId(), "evaluationSetId");
        requirePositive(command.assetId(), "assetId");
    }

    private static void requireValidReportCandidate(CreateAiEvaluationReportCandidateCommand command) {
        if (command == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
        requireValidActor(command.adminId(), command.memberRole(), command.adminRole());
        requirePositive(command.evaluationSetId(), "evaluationSetId");
        requirePositive(command.reportId(), "reportId");
    }

    private static void requireValidPageQuery(Long adminId, String memberRole, String adminRole, int page, int size) {
        requireValidActor(adminId, memberRole, adminRole);
        if (page < 0) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "page must not be negative");
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "size must be between 1 and 100");
        }
    }

    private static void requireValidGetSet(GetAiEvaluationSetQuery query) {
        if (query == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
        requireValidActor(query.adminId(), query.memberRole(), query.adminRole());
        requirePositive(query.setId(), "setId");
    }

    private static void requireValidSetStatusCommand(ChangeAiEvaluationSetStatusCommand command) {
        if (command == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
        requireValidActor(command.adminId(), command.memberRole(), command.adminRole());
        requirePositive(command.setId(), "setId");
    }

    private static void requireValidListCases(ListAiEvaluationCasesQuery query) {
        if (query == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
        requireValidPageQuery(query.adminId(), query.memberRole(), query.adminRole(), query.page(), query.size());
        requirePositive(query.evaluationSetId(), "evaluationSetId");
    }

    private static void requireValidGetCase(GetAiEvaluationCaseQuery query) {
        if (query == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
        requireValidActor(query.adminId(), query.memberRole(), query.adminRole());
        requirePositive(query.caseId(), "caseId");
    }

    private static void requireValidCaseStatusCommand(ChangeAiEvaluationCaseStatusCommand command) {
        if (command == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
        requireValidReviewerActor(command.adminId(), command.memberRole(), command.adminRole());
        requirePositive(command.caseId(), "caseId");
        requireText(command.reviewReason(), "reviewReason");
        if (command.reviewedAt() == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "reviewedAt must not be null");
        }
    }

    private static void requireValidActor(Long adminId, String memberRole, String adminRole) {
        requirePositive(adminId, "adminId");
        requireText(memberRole, "memberRole");
        requireText(adminRole, "adminRole");
        if (!"ADMIN".equals(memberRole)
                || !("REVIEWER".equals(adminRole)
                || "CONTENT_CREATOR".equals(adminRole)
                || "SUPER_ADMIN".equals(adminRole))) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
    }

    private static void requireValidReviewerActor(Long adminId, String memberRole, String adminRole) {
        requirePositive(adminId, "adminId");
        requireText(memberRole, "memberRole");
        requireText(adminRole, "adminRole");
        if (!"ADMIN".equals(memberRole) || !("REVIEWER".equals(adminRole) || "SUPER_ADMIN".equals(adminRole))) {
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

    private static PageRequest pageRequest(int page, int size) {
        return PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")
                .and(Sort.by(Sort.Direction.DESC, "id")));
    }

    private static <E extends Enum<E>> E parseEnum(Class<E> enumType, String value, String fieldName) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return parseEnumRequired(enumType, value, fieldName);
    }

    private static <E extends Enum<E>> E parseEnumRequired(Class<E> enumType, String value, String fieldName) {
        try {
            return Enum.valueOf(enumType, requireText(value, fieldName));
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, fieldName + " is not supported");
        }
    }
}
