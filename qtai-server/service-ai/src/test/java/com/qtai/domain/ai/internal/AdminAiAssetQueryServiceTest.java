package com.qtai.domain.ai.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qtai.domain.ai.api.admin.asset.dto.AdminAiAssetDetailResponse;
import com.qtai.domain.ai.api.admin.asset.dto.GetAdminAiAssetQuery;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AdminAiAssetQueryServiceTest {

    @Mock
    private AdminAiAssetQueryRepository repository;

    @Test
    @DisplayName("detail response includes active generation job for the same target")
    void getAdminAiAsset_mapsActiveGenerationJob() {
        AdminAiAssetQueryService service = new AdminAiAssetQueryService(
                repository,
                new ObjectMapper().findAndRegisterModules()
        );
        OffsetDateTime now = OffsetDateTime.parse("2026-06-12T10:00:00+09:00");
        AdminAiAssetQueryRepository.AdminAiAssetDetailRow detail = new AdminAiAssetQueryRepository.AdminAiAssetDetailRow(
                10L,
                AiGeneratedAssetType.EXPLANATION,
                AiTargetType.BIBLE_VERSE,
                100L,
                AiGeneratedAssetStatus.APPROVED,
                "{}",
                "source",
                now,
                now.plusMinutes(1),
                20L,
                AiGenerationJobType.EXPLANATION,
                AiTargetType.BIBLE_VERSE,
                100L,
                30L,
                AiGenerationJobStatus.SUCCEEDED,
                now.minusMinutes(5),
                now.minusMinutes(4),
                now.minusMinutes(3),
                null,
                30L,
                AiPromptType.EXPLANATION,
                "v1",
                AiPromptVersionStatus.ACTIVE
        );
        AdminAiAssetQueryRepository.AdminAiGenerationJobRow activeJob =
                new AdminAiAssetQueryRepository.AdminAiGenerationJobRow(
                        21L,
                        AiGenerationJobType.EXPLANATION,
                        AiTargetType.BIBLE_VERSE,
                        100L,
                        31L,
                        AiGenerationJobStatus.RUNNING,
                        now.plusMinutes(2),
                        now.plusMinutes(3),
                        null,
                        null
                );

        when(repository.findDetail(10L)).thenReturn(Optional.of(detail));
        when(repository.findActiveGenerationJob(
                AiGenerationJobType.EXPLANATION,
                AiTargetType.BIBLE_VERSE,
                100L
        )).thenReturn(Optional.of(activeJob));
        when(repository.findValidationLogs(10L)).thenReturn(List.of());

        AdminAiAssetDetailResponse response = service.getAdminAiAsset(
                new GetAdminAiAssetQuery(1L, "ADMIN", "REVIEWER", 10L)
        );

        assertThat(response.generationJob().id()).isEqualTo(20L);
        assertThat(response.activeGenerationJob()).isNotNull();
        assertThat(response.activeGenerationJob().id()).isEqualTo(21L);
        assertThat(response.activeGenerationJob().status()).isEqualTo("RUNNING");
        assertThat(response.activeGenerationJob().promptVersionId()).isEqualTo(31L);
        verify(repository).findActiveGenerationJob(
                AiGenerationJobType.EXPLANATION,
                AiTargetType.BIBLE_VERSE,
                100L
        );
    }
}
