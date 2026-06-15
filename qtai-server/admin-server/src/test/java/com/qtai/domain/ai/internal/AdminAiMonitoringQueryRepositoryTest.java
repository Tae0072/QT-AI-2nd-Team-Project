package com.qtai.domain.ai.internal;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AdminAiMonitoringQueryRepositoryTest {

    private static final OffsetDateTime NOW = OffsetDateTime.parse("2026-06-12T10:00:00+09:00");

    @Autowired
    AdminAiMonitoringQueryRepository repository;

    @Autowired
    EntityManager entityManager;

    @Test
    @DisplayName("AI 운영 모니터링은 산출물 상태별 카운트를 별도로 집계한다")
    void summarizeCountsAssetStatuses() {
        asset(1001L, AiGeneratedAssetStatus.VALIDATING);
        asset(1002L, AiGeneratedAssetStatus.APPROVED);
        asset(1003L, AiGeneratedAssetStatus.REJECTED);
        asset(1004L, AiGeneratedAssetStatus.HIDDEN);
        entityManager.flush();
        entityManager.clear();

        AdminAiMonitoringQueryRepository.Summary summary = repository.summarize(filter());

        assertThat(summary.assetStatuses().validating()).isEqualTo(1);
        assertThat(summary.assetStatuses().approved()).isEqualTo(1);
        assertThat(summary.assetStatuses().rejected()).isEqualTo(1);
        assertThat(summary.assetStatuses().hidden()).isEqualTo(1);
    }

    private AiGeneratedAsset asset(Long targetId, AiGeneratedAssetStatus status) {
        AiGenerationJob job = job(targetId);
        entityManager.flush();

        AiGeneratedAsset asset = AiGeneratedAsset.create(
                job.getId(),
                AiGeneratedAssetType.EXPLANATION,
                AiTargetType.BIBLE_VERSE,
                targetId,
                "{\"explanations\":[],\"glossaryTerms\":[]}",
                "test-source",
                NOW.plusSeconds(targetId)
        );
        switch (status) {
            case VALIDATING -> {
            }
            case APPROVED -> asset.approve(NOW.plusMinutes(1));
            case REJECTED -> asset.reject(NOW.plusMinutes(1));
            case HIDDEN -> asset.hide(NOW.plusMinutes(1));
        }
        entityManager.persist(asset);
        return asset;
    }

    private AiGenerationJob job(Long targetId) {
        AiPromptVersion promptVersion = AiPromptVersion.of(
                targetId,
                AiPromptType.EXPLANATION,
                "v" + targetId,
                "hash-" + targetId,
                AiPromptVersionStatus.ACTIVE,
                NOW
        );
        promptVersion = entityManager.merge(promptVersion);

        AiGenerationJob job = AiGenerationJob.queue(
                AiGenerationJobType.EXPLANATION,
                AiTargetType.BIBLE_VERSE,
                targetId,
                promptVersion.getId(),
                NOW
        );
        entityManager.persist(job);
        return job;
    }

    private static AdminAiMonitoringQueryRepository.Filter filter() {
        return new AdminAiMonitoringQueryRepository.Filter(
                NOW.minusDays(1),
                NOW.plusDays(1),
                LocalDateTime.of(2026, 6, 11, 0, 0),
                LocalDateTime.of(2026, 6, 13, 0, 0)
        );
    }
}
