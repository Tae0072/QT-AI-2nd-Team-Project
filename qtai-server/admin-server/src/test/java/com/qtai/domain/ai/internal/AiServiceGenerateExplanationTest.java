package com.qtai.domain.ai.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.qtai.common.exception.BusinessException;
import com.qtai.domain.ai.api.admin.asset.dto.GenerateQtPassageExplanationCommand;
import com.qtai.domain.ai.api.admin.asset.dto.GenerateQtPassageExplanationResult;
import com.qtai.domain.audit.api.WriteAuditLogUseCase;
import com.qtai.domain.audit.api.dto.AuditLogWriteRequest;

/**
 * 관리자 해설 생성 트리거 오케스트레이션 단위 테스트 (F-02/F-06/F-14).
 * 시딩 위임(requestedBy 표기)과 감사 로그 기록(action/target/actor)을 검증한다.
 */
@ExtendWith(MockitoExtension.class)
class AiServiceGenerateExplanationTest {

    @Mock
    AiGenerationJobRepository generationJobRepository;
    @Mock
    AiGeneratedAssetRepository generatedAssetRepository;
    @Mock
    AiPromptVersionRepository promptVersionRepository;
    @Mock
    AiDailyQtVerseExplanationSeedService explanationSeedService;
    @Mock
    WriteAuditLogUseCase auditLogUseCase;

    private AiService aiService() {
        return new AiService(
                generationJobRepository,
                generatedAssetRepository,
                promptVersionRepository,
                explanationSeedService,
                auditLogUseCase,
                new ObjectMapper()
        );
    }

    @Test
    @DisplayName("해설 생성 트리거는 본문 시딩에 위임하고 ADMIN:adminId로 요청 주체를 표기한다")
    void delegates_to_seed_with_admin_requested_by() {
        when(explanationSeedService.seedForPassage(eq(35L), eq("ADMIN:2")))
                .thenReturn(new AiDailyQtVerseExplanationSeedResult(3, 1));

        GenerateQtPassageExplanationResult result = aiService().generateQtPassageExplanation(
                new GenerateQtPassageExplanationCommand(35L, 2L, OffsetDateTime.now()));

        assertThat(result.createdCount()).isEqualTo(3);
        assertThat(result.failedCount()).isEqualTo(1);
        verify(explanationSeedService).seedForPassage(35L, "ADMIN:2");
    }

    @Test
    @DisplayName("해설 생성 트리거는 AI_EXPLANATION_GENERATE_REQUEST 감사 로그를 QT_PASSAGE 대상으로 남긴다")
    void writes_audit_log_for_generate_request() {
        when(explanationSeedService.seedForPassage(eq(35L), eq("ADMIN:2")))
                .thenReturn(new AiDailyQtVerseExplanationSeedResult(2, 0));

        aiService().generateQtPassageExplanation(
                new GenerateQtPassageExplanationCommand(35L, 2L, OffsetDateTime.now()));

        ArgumentCaptor<AuditLogWriteRequest> captor = ArgumentCaptor.forClass(AuditLogWriteRequest.class);
        verify(auditLogUseCase).write(captor.capture());
        AuditLogWriteRequest audit = captor.getValue();
        assertThat(audit.actionType()).isEqualTo("AI_EXPLANATION_GENERATE_REQUEST");
        assertThat(audit.targetType()).isEqualTo("QT_PASSAGE");
        assertThat(audit.targetId()).isEqualTo(35L);
        assertThat(audit.actorId()).isEqualTo(2L);
        assertThat(audit.actorType()).isEqualTo("ADMIN");
    }

    @Test
    @DisplayName("잘못된 입력(qtPassageId<=0)은 시딩/감사 없이 거부된다")
    void invalid_command_is_rejected() {
        assertThatThrownBy(() -> aiService().generateQtPassageExplanation(
                new GenerateQtPassageExplanationCommand(0L, 2L, OffsetDateTime.now())))
                .isInstanceOf(BusinessException.class);

        verify(explanationSeedService, never()).seedForPassage(eq(0L), eq("ADMIN:2"));
        verify(auditLogUseCase, never()).write(org.mockito.ArgumentMatchers.any());
    }
}
