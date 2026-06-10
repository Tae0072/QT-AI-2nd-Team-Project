package com.qtai.domain.ai.internal;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import java.time.OffsetDateTime;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qtai.common.exception.BusinessException;
import com.qtai.domain.ai.api.generation.dto.CreateAiGenerationJobCommand;
import com.qtai.domain.audit.api.WriteAuditLogUseCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * {@link AiService#createAiGenerationJob} 가드 분기 단위 테스트 (claude-review 커버리지 지적 반영).
 *
 * <p>아래 분기들은 저장소 접근 이전에 동작하므로 저장소는 호출되지 않아야 한다(상호작용 없음 검증).
 * <ul>
 *   <li>SIMULATOR 생성은 사용자 요청 경로에서 즉시 거부(CLAUDE.md §6: 시뮬레이터는 사전 제작/검증).</li>
 *   <li>잘못된 입력(null·미지원 jobType)은 BusinessException으로 감싼다.</li>
 * </ul>
 */
class AiServiceTest {

    private final AiGenerationJobRepository generationJobRepository = mock(AiGenerationJobRepository.class);
    private final AiGeneratedAssetRepository generatedAssetRepository = mock(AiGeneratedAssetRepository.class);
    private final AiPromptVersionRepository promptVersionRepository = mock(AiPromptVersionRepository.class);
    private final WriteAuditLogUseCase auditLogUseCase = mock(WriteAuditLogUseCase.class);

    private final AiService aiService = new AiService(
            generationJobRepository,
            generatedAssetRepository,
            promptVersionRepository,
            auditLogUseCase,
            new ObjectMapper());

    private CreateAiGenerationJobCommand command(String jobType) {
        return new CreateAiGenerationJobCommand(
                jobType, "BIBLE_VERSE", 1L, 1L, "tester", OffsetDateTime.now());
    }

    @Test
    @DisplayName("SIMULATOR 생성 요청은 저장소 접근 없이 즉시 거부된다")
    void simulator_generation_is_rejected_before_any_repository_access() {
        assertThatThrownBy(() -> aiService.createAiGenerationJob(command("SIMULATOR")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("SIMULATOR_GENERATION_NOT_SUPPORTED");

        verifyNoInteractions(generationJobRepository, generatedAssetRepository, promptVersionRepository);
    }

    @Test
    @DisplayName("null command는 INVALID_INPUT으로 거부된다")
    void null_command_is_rejected() {
        assertThatThrownBy(() -> aiService.createAiGenerationJob(null))
                .isInstanceOf(BusinessException.class);
        verifyNoInteractions(generationJobRepository, promptVersionRepository);
    }

    @Test
    @DisplayName("미지원 jobType 문자열은 저장소 접근 없이 거부된다")
    void unsupported_job_type_is_rejected() {
        assertThatThrownBy(() -> aiService.createAiGenerationJob(command("NOT_A_REAL_TYPE")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("jobType");

        verifyNoInteractions(generationJobRepository, promptVersionRepository);
    }
}
