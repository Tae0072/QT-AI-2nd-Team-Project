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
import java.util.Optional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qtai.common.exception.BusinessException;
import com.qtai.common.exception.ErrorCode;
import com.qtai.domain.ai.api.admin.evaluation.dto.ChangeAiEvaluationCaseStatusCommand;
import com.qtai.domain.ai.api.admin.evaluation.dto.ChangeAiEvaluationSetStatusCommand;
import com.qtai.domain.ai.api.admin.evaluation.dto.CreateAiEvaluationSetCommand;
import com.qtai.domain.ai.api.admin.evaluation.dto.CreateAiEvaluationAssetCandidateCommand;
import com.qtai.domain.audit.api.WriteAuditLogUseCase;
import com.qtai.domain.audit.api.dto.AuditLogWriteRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class AiEvaluationServiceTest {

    private static final OffsetDateTime NOW = OffsetDateTime.parse("2026-06-11T10:00:00+09:00");

    private AiEvaluationSetRepository setRepository;
    private AiEvaluationCaseRepository caseRepository;
    private AiGeneratedAssetRepository assetRepository;
    private WriteAuditLogUseCase auditLogUseCase;
    private AiEvaluationService service;

    @BeforeEach
    void setUp() {
        setRepository = mock(AiEvaluationSetRepository.class);
        caseRepository = mock(AiEvaluationCaseRepository.class);
        assetRepository = mock(AiGeneratedAssetRepository.class);
        auditLogUseCase = mock(WriteAuditLogUseCase.class);
        service = new AiEvaluationService(
                setRepository,
                caseRepository,
                assetRepository,
                auditLogUseCase,
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
