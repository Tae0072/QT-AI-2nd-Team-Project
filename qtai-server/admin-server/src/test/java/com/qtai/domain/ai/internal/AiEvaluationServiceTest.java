package com.qtai.domain.ai.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qtai.common.exception.BusinessException;
import com.qtai.common.exception.ErrorCode;
import com.qtai.domain.ai.api.admin.evaluation.dto.ChangeAiEvaluationCaseStatusCommand;
import com.qtai.domain.ai.api.admin.evaluation.dto.ChangeAiEvaluationSetStatusCommand;
import com.qtai.domain.ai.api.admin.evaluation.dto.CreateAiEvaluationCaseCommand;
import com.qtai.domain.ai.api.admin.evaluation.dto.CreateAiEvaluationSetCommand;
import com.qtai.domain.ai.api.admin.evaluation.dto.CreateAiEvaluationAssetCandidateCommand;
import com.qtai.domain.ai.api.admin.evaluation.dto.CreateAiEvaluationReportCandidateCommand;
import com.qtai.domain.ai.api.admin.evaluation.dto.GetAiEvaluationCaseQuery;
import com.qtai.domain.ai.api.admin.evaluation.dto.GetAiEvaluationSetQuery;
import com.qtai.domain.ai.api.admin.evaluation.dto.ListAiEvaluationCasesQuery;
import com.qtai.domain.ai.api.admin.evaluation.dto.ListAiEvaluationSetsQuery;
import com.qtai.domain.audit.api.WriteAuditLogUseCase;
import com.qtai.domain.audit.api.dto.AuditLogWriteRequest;
import com.qtai.domain.report.api.GetReportUseCase;
import com.qtai.domain.report.api.dto.ReportForEvaluation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

class AiEvaluationServiceTest {

    private static final OffsetDateTime NOW = OffsetDateTime.parse("2026-06-11T10:00:00+09:00");

    private AiEvaluationSetRepository setRepository;
    private AiEvaluationCaseRepository caseRepository;
    private AiGeneratedAssetRepository assetRepository;
    private WriteAuditLogUseCase auditLogUseCase;
    private GetReportUseCase getReportUseCase;
    private AiEvaluationService service;

    @BeforeEach
    void setUp() {
        setRepository = mock(AiEvaluationSetRepository.class);
        caseRepository = mock(AiEvaluationCaseRepository.class);
        assetRepository = mock(AiGeneratedAssetRepository.class);
        auditLogUseCase = mock(WriteAuditLogUseCase.class);
        getReportUseCase = mock(GetReportUseCase.class);
        service = new AiEvaluationService(
                setRepository,
                caseRepository,
                assetRepository,
                auditLogUseCase,
                getReportUseCase,
                new ObjectMapper().findAndRegisterModules(),
                Clock.fixed(Instant.parse("2026-06-11T01:00:00Z"), ZoneId.of("Asia/Seoul"))
        );
    }

    @Test
    void createSetRejectsDuplicateEvalTypeVersion() {
        when(setRepository.existsByEvalTypeAndVersion(AiEvaluationType.QA, "2026.06.1"))
                .thenReturn(true);

        assertThatThrownBy(() -> service.createEvaluationSet(setCommand()))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.DUPLICATE_RESOURCE);
    }

    @Test
    void listEvaluationSetsReturnsFilteredPage() {
        when(setRepository.findByEvalTypeAndTargetTypeAndStatus(
                any(), any(), any(), any(Pageable.class)
        )).thenReturn(new PageImpl<>(List.of(evaluationSet())));

        var response = service.listEvaluationSets(new ListAiEvaluationSetsQuery(
                7L,
                "ADMIN",
                "CONTENT_CREATOR",
                "QA",
                "QA_REQUEST",
                "DRAFT",
                0,
                20
        ));

        assertThat(response.content()).hasSize(1);
        assertThat(response.content().get(0).id()).isEqualTo(20L);
    }

    @Test
    void listEvaluationSetsRejectsUnsupportedEnum() {
        assertThatThrownBy(() -> service.listEvaluationSets(new ListAiEvaluationSetsQuery(
                7L,
                "ADMIN",
                "REVIEWER",
                "UNSUPPORTED",
                null,
                null,
                0,
                20
        )))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_INPUT);
    }

    @Test
    void getEvaluationSetReturnsDetail() {
        when(setRepository.findById(20L)).thenReturn(Optional.of(evaluationSet()));

        var response = service.getEvaluationSet(new GetAiEvaluationSetQuery(
                7L,
                "ADMIN",
                "CONTENT_CREATOR",
                20L
        ));

        assertThat(response.id()).isEqualTo(20L);
        assertThat(response.evalType()).isEqualTo("QA");
    }

    @Test
    void activateSetRequiresAtLeastTenApprovedCases() {
        AiEvaluationSet set = AiEvaluationSet.create(
                "절별 해설 평가",
                AiEvaluationType.EXPLANATION,
                "2026.06.1",
                AiTargetType.BIBLE_VERSE,
                "{\"expectedResult\":\"REJECTED\"}",
                "평가 셋",
                NOW
        );
        setId(set, 20L);
        when(setRepository.findById(20L)).thenReturn(Optional.of(set));
        when(caseRepository.countByEvaluationSetIdAndStatus(20L, AiEvaluationCaseStatus.APPROVED))
                .thenReturn(9L);

        assertThatThrownBy(() -> service.activateEvaluationSet(statusCommand(20L)))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_STATUS_TRANSITION);
    }

    @Test
    void activateAndRetireSetChangeStatus() {
        AiEvaluationSet set = evaluationSet();
        when(setRepository.findById(20L)).thenReturn(Optional.of(set));
        when(caseRepository.countByEvaluationSetIdAndStatus(20L, AiEvaluationCaseStatus.APPROVED))
                .thenReturn(10L);

        var activated = service.activateEvaluationSet(statusCommand(20L));
        var retired = service.retireEvaluationSet(statusCommand(20L));

        assertThat(activated.status()).isEqualTo("ACTIVE");
        assertThat(retired.status()).isEqualTo("RETIRED");
    }

    @Test
    void createEvaluationCaseStoresIdentifierMetadataOnly() {
        when(setRepository.findById(20L)).thenReturn(Optional.of(evaluationSet()));
        when(caseRepository.save(any())).thenAnswer(invocation -> {
            AiEvaluationCase saved = invocation.getArgument(0);
            setId(saved, 303L);
            return saved;
        });

        // 자유 텍스트(inputJson/expectedOutput)를 보내도 서버는 무시하고 식별자·메타만 저장한다.
        var response = service.createEvaluationCase(new CreateAiEvaluationCaseCommand(
                7L,
                "ADMIN",
                "CONTENT_CREATOR",
                20L,
                "QA_REQUEST",
                1001L,
                "ADMIN_CREATED",
                null,
                "{\"question\":\"must not store raw\"}",
                "{\"answer\":\"must not store raw\"}",
                "{\"expectedResult\":\"REJECTED\"}",
                "CANDIDATE"
        ));

        assertThat(response.id()).isEqualTo(303L);
        assertThat(response.status()).isEqualTo("CANDIDATE");
        ArgumentCaptor<AiEvaluationCase> captor = ArgumentCaptor.forClass(AiEvaluationCase.class);
        verify(caseRepository).save(captor.capture());
        AiEvaluationCase saved = captor.getValue();
        assertThat(saved.getInputJson())
                .contains("\"targetType\":\"QA_REQUEST\"")
                .contains("\"targetId\":1001")
                .doesNotContain("question")
                .doesNotContain("must not store raw");
        assertThat(saved.getSourceType()).isEqualTo(AiEvaluationSourceType.ADMIN_CREATED);
        assertThat(saved.getExpectedOutputJson()).isNull();
    }

    @Test
    void createEvaluationCaseRequiresTargetId() {
        when(setRepository.findById(20L)).thenReturn(Optional.of(evaluationSet()));

        assertThatThrownBy(() -> service.createEvaluationCase(new CreateAiEvaluationCaseCommand(
                7L, "ADMIN", "CONTENT_CREATOR", 20L, "QA_REQUEST", null, "ADMIN_CREATED",
                null, null, null, null, "CANDIDATE")))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_INPUT);
    }

    @Test
    void listAndGetEvaluationCasesReturnResponses() {
        AiEvaluationCase evaluationCase = evaluationCase();
        when(caseRepository.findByEvaluationSetId(any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(evaluationCase)));
        when(caseRepository.findById(301L)).thenReturn(Optional.of(evaluationCase));

        var list = service.listEvaluationCases(new ListAiEvaluationCasesQuery(
                7L,
                "ADMIN",
                "CONTENT_CREATOR",
                20L,
                null,
                0,
                20
        ));
        var detail = service.getEvaluationCase(new GetAiEvaluationCaseQuery(
                7L,
                "ADMIN",
                "REVIEWER",
                301L
        ));

        assertThat(list.content()).hasSize(1);
        assertThat(detail.id()).isEqualTo(301L);
    }

    @Test
    void approveCaseChangesStatusAndWritesAudit() {
        AiEvaluationCase evaluationCase = evaluationCase();
        when(caseRepository.findById(301L)).thenReturn(Optional.of(evaluationCase));

        service.approveEvaluationCase(caseStatusCommand(301L));

        assertThat(evaluationCase.getStatus()).isEqualTo(AiEvaluationCaseStatus.APPROVED);
        assertThat(evaluationCase.getReviewedByAdminId()).isEqualTo(7L);
        ArgumentCaptor<AuditLogWriteRequest> captor = ArgumentCaptor.forClass(AuditLogWriteRequest.class);
        verify(auditLogUseCase).write(captor.capture());
        assertThat(captor.getValue().actionType()).isEqualTo("EVAL_CASE_APPROVE");
        assertThat(captor.getValue().targetType()).isEqualTo("AI_EVALUATION_CASE");
        assertThat(captor.getValue().targetId()).isEqualTo(301L);
        assertThat(captor.getValue().afterJson()).contains("\"reviewReason\":\"review reason\"");
    }

    @Test
    void contentCreatorCannotApproveCaseInService() {
        assertThatThrownBy(() -> service.approveEvaluationCase(new ChangeAiEvaluationCaseStatusCommand(
                7L,
                "ADMIN",
                "CONTENT_CREATOR",
                301L,
                "review reason",
                NOW
        )))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.FORBIDDEN);
    }

    @Test
    void rejectProcessedCaseFails() {
        AiEvaluationCase evaluationCase = evaluationCase();
        evaluationCase.approve(7L, NOW);
        when(caseRepository.findById(301L)).thenReturn(Optional.of(evaluationCase));

        assertThatThrownBy(() -> service.rejectEvaluationCase(caseStatusCommand(301L)))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_STATUS_TRANSITION);
    }

    @Test
    void createAssetCandidateUsesSanitizedAssetSnapshot() {
        AiEvaluationSet set = AiEvaluationSet.create(
                "절별 해설 평가",
                AiEvaluationType.EXPLANATION,
                "2026.06.1",
                AiTargetType.BIBLE_VERSE,
                "{\"expectedResult\":\"REJECTED\"}",
                "평가 셋",
                NOW
        );
        setId(set, 20L);
        AiGeneratedAsset asset = AiGeneratedAsset.create(
                11L,
                AiGeneratedAssetType.EXPLANATION,
                AiTargetType.BIBLE_VERSE,
                1001L,
                "{\"explanations\":[{\"verseId\":1001,\"summary\":\"must not copy\"}]}",
                "QT-AI DeepSeek",
                NOW
        );
        setId(asset, 500L);
        when(setRepository.findById(20L)).thenReturn(Optional.of(set));
        when(assetRepository.findById(500L)).thenReturn(Optional.of(asset));
        when(caseRepository.save(any())).thenAnswer(invocation -> {
            AiEvaluationCase saved = invocation.getArgument(0);
            setId(saved, 302L);
            return saved;
        });

        var response = service.createAssetCandidate(new CreateAiEvaluationAssetCandidateCommand(
                7L,
                "ADMIN",
                "REVIEWER",
                20L,
                500L,
                "{\"expectedResult\":\"REJECTED\"}"
        ));

        assertThat(response.id()).isEqualTo(302L);
        ArgumentCaptor<AiEvaluationCase> captor = ArgumentCaptor.forClass(AiEvaluationCase.class);
        verify(caseRepository).save(captor.capture());
        assertThat(captor.getValue().getInputJson()).contains("\"assetId\":500");
        assertThat(captor.getValue().getInputJson()).doesNotContain("explanations");
    }

    @Test
    void createReportCandidateFromQaRequestUsesMetadataOnlySnapshot() {
        when(setRepository.findById(20L)).thenReturn(Optional.of(evaluationSet())); // QA_REQUEST set
        when(getReportUseCase.getReportForEvaluation(789L)).thenReturn(
                new ReportForEvaluation(789L, "AI_QA_REQUEST", 700L, "FACT_ERROR", "RECEIVED", 999L));
        when(caseRepository.save(any())).thenAnswer(invocation -> {
            AiEvaluationCase saved = invocation.getArgument(0);
            setId(saved, 305L);
            return saved;
        });

        var response = service.createReportCandidate(new CreateAiEvaluationReportCandidateCommand(
                7L, "ADMIN", "REVIEWER", 20L, 789L, "{\"expectedResult\":\"REJECTED\"}"));

        assertThat(response.id()).isEqualTo(305L);
        ArgumentCaptor<AiEvaluationCase> captor = ArgumentCaptor.forClass(AiEvaluationCase.class);
        verify(caseRepository).save(captor.capture());
        AiEvaluationCase saved = captor.getValue();
        assertThat(saved.getSourceType()).isEqualTo(AiEvaluationSourceType.USER_REPORT);
        assertThat(saved.getSourceId()).isEqualTo(789L);
        assertThat(saved.getTargetType()).isEqualTo(AiTargetType.QA_REQUEST);
        assertThat(saved.getTargetId()).isEqualTo(700L);
        assertThat(saved.getInputJson()).contains("\"reportId\":789").contains("\"reason\":\"FACT_ERROR\"");
        ArgumentCaptor<AuditLogWriteRequest> auditCaptor = ArgumentCaptor.forClass(AuditLogWriteRequest.class);
        verify(auditLogUseCase).write(auditCaptor.capture());
        assertThat(auditCaptor.getValue().actionType()).isEqualTo("EVAL_CASE_REPORT_CANDIDATE");
        assertThat(auditCaptor.getValue().targetType()).isEqualTo("AI_EVALUATION_CASE");
        assertThat(auditCaptor.getValue().targetId()).isEqualTo(305L);
    }

    @Test
    void createReportCandidateDuplicateGuardRejects() {
        when(setRepository.findById(20L)).thenReturn(Optional.of(evaluationSet()));
        when(getReportUseCase.getReportForEvaluation(789L)).thenReturn(
                new ReportForEvaluation(789L, "AI_QA_REQUEST", 700L, "FACT_ERROR", "RECEIVED", 999L));
        when(caseRepository.existsBySourceTypeAndSourceId(AiEvaluationSourceType.USER_REPORT, 789L)).thenReturn(true);

        assertThatThrownBy(() -> service.createReportCandidate(new CreateAiEvaluationReportCandidateCommand(
                7L, "ADMIN", "REVIEWER", 20L, 789L, null)))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.DUPLICATE_RESOURCE);
    }

    @Test
    void createReportCandidateFromAssetReportEnrichesLinkedAssetMeta() {
        AiEvaluationSet bibleSet = AiEvaluationSet.create(
                "절별 해설 평가", AiEvaluationType.EXPLANATION, "2026.06.1",
                AiTargetType.BIBLE_VERSE, "{\"expectedResult\":\"REJECTED\"}", "평가 셋", NOW);
        setId(bibleSet, 21L);
        AiGeneratedAsset asset = AiGeneratedAsset.create(
                11L, AiGeneratedAssetType.EXPLANATION, AiTargetType.BIBLE_VERSE, 1001L,
                "{\"explanations\":[{\"verseId\":1001,\"summary\":\"must not copy\"}]}", "QT-AI DeepSeek", NOW);
        setId(asset, 500L);
        when(setRepository.findById(21L)).thenReturn(Optional.of(bibleSet));
        when(getReportUseCase.getReportForEvaluation(790L)).thenReturn(
                new ReportForEvaluation(790L, "AI_ASSET", 500L, "FACT_ERROR", "RECEIVED", 999L));
        when(assetRepository.findById(500L)).thenReturn(Optional.of(asset));
        when(caseRepository.save(any())).thenAnswer(invocation -> {
            AiEvaluationCase saved = invocation.getArgument(0);
            setId(saved, 306L);
            return saved;
        });

        service.createReportCandidate(new CreateAiEvaluationReportCandidateCommand(
                7L, "ADMIN", "REVIEWER", 21L, 790L, null));

        ArgumentCaptor<AiEvaluationCase> captor = ArgumentCaptor.forClass(AiEvaluationCase.class);
        verify(caseRepository).save(captor.capture());
        AiEvaluationCase saved = captor.getValue();
        assertThat(saved.getTargetType()).isEqualTo(AiTargetType.BIBLE_VERSE);
        assertThat(saved.getInputJson()).contains("\"linkedAssetId\":500");
        assertThat(saved.getInputJson()).doesNotContain("explanations"); // 원문 미저장
    }

    @Test
    void createReportCandidateRejectsNonAiReport() {
        when(setRepository.findById(20L)).thenReturn(Optional.of(evaluationSet()));
        when(getReportUseCase.getReportForEvaluation(791L)).thenReturn(
                new ReportForEvaluation(791L, "POST", 300L, "SPAM", "RECEIVED", 999L));

        assertThatThrownBy(() -> service.createReportCandidate(new CreateAiEvaluationReportCandidateCommand(
                7L, "ADMIN", "REVIEWER", 20L, 791L, null)))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_INPUT);
    }

    @Test
    void createReportCandidateRejectsTargetTypeMismatch() {
        AiEvaluationSet bibleSet = AiEvaluationSet.create(
                "절별 해설 평가", AiEvaluationType.EXPLANATION, "2026.06.1",
                AiTargetType.BIBLE_VERSE, "{\"expectedResult\":\"REJECTED\"}", "평가 셋", NOW);
        setId(bibleSet, 21L);
        when(setRepository.findById(21L)).thenReturn(Optional.of(bibleSet));
        when(getReportUseCase.getReportForEvaluation(792L)).thenReturn(
                new ReportForEvaluation(792L, "AI_QA_REQUEST", 700L, "FACT_ERROR", "RECEIVED", 999L));

        assertThatThrownBy(() -> service.createReportCandidate(new CreateAiEvaluationReportCandidateCommand(
                7L, "ADMIN", "REVIEWER", 21L, 792L, null)))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_INPUT);
    }

    private static CreateAiEvaluationSetCommand setCommand() {
        return new CreateAiEvaluationSetCommand(
                7L,
                "ADMIN",
                "REVIEWER",
                "AI Q&A 정책 평가",
                "QA",
                "2026.06.1",
                "QA_REQUEST",
                "{\"blocked\":\"VALUE_JUDGMENT\"}",
                "평가 셋",
                "DRAFT"
        );
    }

    private static ChangeAiEvaluationSetStatusCommand statusCommand(Long setId) {
        return new ChangeAiEvaluationSetStatusCommand(7L, "ADMIN", "REVIEWER", setId);
    }

    private static ChangeAiEvaluationCaseStatusCommand caseStatusCommand(Long caseId) {
        return new ChangeAiEvaluationCaseStatusCommand(7L, "ADMIN", "REVIEWER", caseId, "review reason", NOW);
    }

    private static AiEvaluationSet evaluationSet() {
        AiEvaluationSet set = AiEvaluationSet.create(
                "AI Q&A 정책 평가",
                AiEvaluationType.QA,
                "2026.06.1",
                AiTargetType.QA_REQUEST,
                "{\"blocked\":\"VALUE_JUDGMENT\"}",
                "평가 셋",
                NOW
        );
        setId(set, 20L);
        return set;
    }

    private static AiEvaluationCase evaluationCase() {
        AiEvaluationCase evaluationCase = AiEvaluationCase.create(
                20L,
                AiTargetType.QA_REQUEST,
                1001L,
                AiEvaluationSourceType.ADMIN_CREATED,
                null,
                "{\"question\":\"test\"}",
                "{\"answer\":\"blocked\"}",
                "{\"expectedResult\":\"REJECTED\"}",
                NOW
        );
        setId(evaluationCase, 301L);
        return evaluationCase;
    }

    private static void setId(Object target, Long id) {
        try {
            Field idField = target.getClass().getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(target, id);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
