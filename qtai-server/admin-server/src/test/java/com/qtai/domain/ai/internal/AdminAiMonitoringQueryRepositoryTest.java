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

    private static final OffsetDateTime FROM = OffsetDateTime.parse("2026-06-15T00:00:00+09:00");
    private static final OffsetDateTime TO = OffsetDateTime.parse("2026-06-16T00:00:00+09:00");
    private static final OffsetDateTime CREATED_AT = OffsetDateTime.parse("2026-06-15T09:00:00+09:00");
    private static final OffsetDateTime REVIEWED_AT = OffsetDateTime.parse("2026-06-15T10:00:00+09:00");

    @Autowired
    AdminAiMonitoringQueryRepository repository;

    @Autowired
    EntityManager entityManager;

    @Test
    @DisplayName("AD-08은 산출물 관리 상태와 검증 로그 결과를 분리 집계한다")
    void summarizeSeparatesReviewedAssetStatusesFromValidationLogResults() {
        AiGeneratedAsset waiting = asset(1001L);
        AiGeneratedAsset approved = asset(1002L);
        approved.approve(REVIEWED_AT);
        AiGeneratedAsset rejected = asset(1003L);
        rejected.reject(REVIEWED_AT.plusMinutes(1));
        AiGeneratedAsset hidden = asset(1004L);
        hidden.hide(REVIEWED_AT.plusMinutes(2));
        addValidationLog(waiting.getId(), AiValidationResult.REJECTED);
        entityManager.flush();
        entityManager.clear();

        AdminAiMonitoringQueryRepository.Summary summary = repository.summarize(filter());

        assertThat(summary.assetStatuses().validating()).isEqualTo(1);
        assertThat(summary.assetStatuses().approved()).isEqualTo(1);
        assertThat(summary.assetStatuses().rejected()).isEqualTo(1);
        assertThat(summary.assetStatuses().hidden()).isEqualTo(1);
        assertThat(summary.validation().waitingAssets()).isEqualTo(1);
        assertThat(summary.validation().approvedAssets()).isEqualTo(1);
        assertThat(summary.validation().rejectedAssets()).isEqualTo(1);
        assertThat(summary.validation().hiddenAssets()).isEqualTo(1);
        assertThat(summary.validation().failCount()).isEqualTo(1);
    }

    private AiGeneratedAsset asset(Long targetId) {
        AiGenerationJob job = job(targetId);
        entityManager.flush();

        AiGeneratedAsset asset = AiGeneratedAsset.create(
                job.getId(),
                AiGeneratedAssetType.EXPLANATION,
                AiTargetType.BIBLE_VERSE,
                targetId,
                "{\"explanations\":[],\"glossaryTerms\":[]}",
                "test-source",
                CREATED_AT.plusSeconds(targetId)
        );
        entityManager.persist(asset);
        entityManager.flush();
        return asset;
    }

    private AiGenerationJob job(Long targetId) {
        AiPromptVersion promptVersion = AiPromptVersion.of(
                targetId,
                AiPromptType.EXPLANATION,
                "v" + targetId,
                "hash-" + targetId,
                AiPromptVersionStatus.ACTIVE,
                CREATED_AT
        );
        promptVersion = entityManager.merge(promptVersion);

        AiGenerationJob job = AiGenerationJob.queue(
                AiGenerationJobType.EXPLANATION,
                AiTargetType.BIBLE_VERSE,
                targetId,
                promptVersion.getId(),
                CREATED_AT
        );
        entityManager.persist(job);
        return job;
    }

    private void addValidationLog(Long assetId, AiValidationResult result) {
        entityManager.persist(AiValidationLog.create(
                assetId,
                null,
                1,
                result,
                AiValidationReviewerType.AUTO,
                4L,
                "{}",
                "AUTO_VALIDATION_REJECTED",
                REVIEWED_AT.plusMinutes(3)
        ));
    }

    private static AdminAiMonitoringQueryRepository.Filter filter() {
        return new AdminAiMonitoringQueryRepository.Filter(
                FROM,
                TO,
                LocalDateTime.of(2026, 6, 15, 0, 0),
                LocalDateTime.of(2026, 6, 16, 0, 0)
        );
    }
}
