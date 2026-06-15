package com.qtai.domain.ai.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.qtai.common.exception.BusinessException;
import com.qtai.domain.ai.api.admin.prompt.dto.AiPromptVersionResponse;
import com.qtai.domain.ai.api.admin.prompt.dto.ChangeAiPromptVersionStatusCommand;
import com.qtai.domain.audit.api.WriteAuditLogUseCase;

@ExtendWith(MockitoExtension.class)
class AiPromptManagementServiceTest {

    private static final OffsetDateTime NOW = OffsetDateTime.parse("2026-06-15T10:00:00+09:00");

    @Mock
    private AiPromptVersionRepository promptVersionRepository;
    @Mock
    private AiEvaluationRunRepository evaluationRunRepository;
    @Mock
    private WriteAuditLogUseCase auditLogUseCase;

    private AiPromptManagementService service;

    @BeforeEach
    void setUp() {
        service = new AiPromptManagementService(
                promptVersionRepository,
                evaluationRunRepository,
                auditLogUseCase,
                new ObjectMapper(),
                Clock.fixed(Instant.parse("2026-06-15T01:00:00Z"), ZoneId.of("Asia/Seoul"))
        );
    }

    @Test
    void activateRequiresSuccessfulEvaluationRun() {
        when(promptVersionRepository.findPromptTypeById(2L)).thenReturn(Optional.of(AiPromptType.EXPLANATION));
        when(evaluationRunRepository.findFirstByPromptVersionIdAndStatusOrderByFinishedAtDescIdDesc(
                2L,
                AiEvaluationRunStatus.SUCCEEDED
        )).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.activateAiPromptVersion(command(2L)))
                .isInstanceOf(BusinessException.class);

        verify(promptVersionRepository, never()).findAllByPromptTypeForUpdate(AiPromptType.EXPLANATION);
    }

    @Test
    void activateRetiresCurrentActivePromptAfterPassingEvaluationRun() {
        AiPromptVersion currentActive = promptVersion(
                1L,
                "2026.06.1",
                AiPromptVersionStatus.ACTIVE,
                NOW.minusDays(2)
        );
        AiPromptVersion draft = promptVersion(
                2L,
                "2026.06.2",
                AiPromptVersionStatus.DRAFT,
                NOW.minusDays(1)
        );
        AiEvaluationRun run = AiEvaluationRun.start(10L, 2L, 99L, NOW.minusHours(1));
        run.finish(2, 2, 0, 0, NOW.minusMinutes(30));

        when(promptVersionRepository.findPromptTypeById(2L)).thenReturn(Optional.of(AiPromptType.EXPLANATION));
        when(evaluationRunRepository.findFirstByPromptVersionIdAndStatusOrderByFinishedAtDescIdDesc(
                2L,
                AiEvaluationRunStatus.SUCCEEDED
        )).thenReturn(Optional.of(run));
        when(promptVersionRepository.findAllByPromptTypeForUpdate(AiPromptType.EXPLANATION))
                .thenReturn(List.of(currentActive, draft));

        AiPromptVersionResponse response = service.activateAiPromptVersion(command(2L));

        assertThat(response.status()).isEqualTo("ACTIVE");
        assertThat(currentActive.getStatus()).isEqualTo(AiPromptVersionStatus.RETIRED);
        assertThat(draft.getStatus()).isEqualTo(AiPromptVersionStatus.ACTIVE);
    }

    private static ChangeAiPromptVersionStatusCommand command(Long promptVersionId) {
        return new ChangeAiPromptVersionStatusCommand(
                99L,
                "ADMIN",
                "REVIEWER",
                promptVersionId
        );
    }

    private static AiPromptVersion promptVersion(
            Long id,
            String version,
            AiPromptVersionStatus status,
            OffsetDateTime createdAt
    ) {
        return AiPromptVersion.of(
                id,
                AiPromptType.EXPLANATION,
                version,
                "hash-" + id,
                status,
                AiPromptVersion.defaultSystemPrompt(),
                AiPromptVersion.defaultUserPromptTemplate(),
                null,
                0.2,
                2000,
                null,
                99L,
                createdAt,
                status == AiPromptVersionStatus.ACTIVE ? createdAt : null,
                null
        );
    }
}
