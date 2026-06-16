package com.qtai.domain.ai.internal;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.qtai.domain.ai.api.admin.asset.dto.ListAdminAiAssetsQuery;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AdminAiAssetQueryRepositoryTest {

    private static final OffsetDateTime NOW = OffsetDateTime.parse("2026-06-12T10:00:00+09:00");

    @Autowired
    AdminAiAssetQueryRepository repository;

    @Autowired
    EntityManager entityManager;

    @Test
    @DisplayName("checklistVersionId 필터가 없으면 count에서 latestValidation 조인을 생략해 전체 asset 수를 반환한다")
    void findAllCountsAllAssetsWithoutChecklistVersionFilter() {
        AiGeneratedAsset first = asset(1001L);
        AiGeneratedAsset second = asset(1002L);
        addValidationLog(
                second.getId(),
                10L,
                1,
                AiValidationResult.PASSED,
                AiValidationReviewerType.AUTO,
                NOW.minusMinutes(2)
        );
        addValidationLog(
                second.getId(),
                20L,
                2,
                AiValidationResult.NEEDS_REVIEW,
                AiValidationReviewerType.ADVISOR,
                NOW.minusMinutes(1)
        );
        entityManager.flush();
        entityManager.clear();

        AdminAiAssetQueryRepository.AdminAiAssetPage page = repository.findAll(
                query(null),
                PageRequest.of(0, 20)
        );

        assertThat(page.totalElements()).isEqualTo(2);
        assertThat(page.content()).extracting(AdminAiAssetQueryRepository.AdminAiAssetListRow::id)
                .containsExactly(second.getId(), first.getId());
        assertThat(page.content().get(0).latestValidationResult()).isEqualTo(AiValidationResult.NEEDS_REVIEW);
        assertThat(page.content().get(0).checklistVersionId()).isEqualTo(20L);
        assertThat(page.content().get(0).autoValidationResult()).isEqualTo(AiValidationResult.PASSED);
        assertThat(page.content().get(0).advisorValidationResult()).isEqualTo(AiValidationResult.NEEDS_REVIEW);
    }

    @Test
    @DisplayName("checklistVersionId 필터는 최신 validation log 기준으로 목록과 count를 함께 제한한다")
    void findAllCountsOnlyAssetsWithLatestChecklistVersionFilter() {
        AiGeneratedAsset first = asset(1001L);
        addValidationLog(first.getId(), 99L, NOW.minusMinutes(2));
        addValidationLog(first.getId(), 10L, NOW.minusMinutes(1));
        AiGeneratedAsset second = asset(1002L);
        addValidationLog(second.getId(), 10L, NOW.minusMinutes(2));
        addValidationLog(second.getId(), 20L, NOW.minusMinutes(1));
        entityManager.flush();
        entityManager.clear();

        AdminAiAssetQueryRepository.AdminAiAssetPage page = repository.findAll(
                query(10L),
                PageRequest.of(0, 20)
        );

        assertThat(page.totalElements()).isEqualTo(1);
        assertThat(page.content()).singleElement().satisfies(row -> {
            assertThat(row.id()).isEqualTo(first.getId());
            assertThat(row.checklistVersionId()).isEqualTo(10L);
            assertThat(row.autoValidationResult()).isEqualTo(AiValidationResult.PASSED);
            assertThat(row.advisorValidationResult()).isNull();
        });
    }

    @Test
    @DisplayName("findActiveGenerationJob은 QUEUED/RUNNING 상태 job만 반환한다")
    void findAllApprovedAssetsOrdersByReviewedAtDescending() {
        AiGeneratedAsset recentlyReviewed = asset(1001L);
        recentlyReviewed.approve(NOW.plusHours(2));
        AiGeneratedAsset olderReviewed = asset(1002L);
        olderReviewed.approve(NOW.plusHours(1));
        entityManager.flush();
        entityManager.clear();

        AdminAiAssetQueryRepository.AdminAiAssetPage page = repository.findAll(
                query("APPROVED", null),
                PageRequest.of(0, 20)
        );

        assertThat(page.content()).extracting(AdminAiAssetQueryRepository.AdminAiAssetListRow::id)
                .containsExactly(recentlyReviewed.getId(), olderReviewed.getId());
    }

    @Test
    @DisplayName("APPROVED 목록은 최근 reviewedAt 순으로 반환한다")
    void findActiveGenerationJobReturnsQueuedOrRunningOnly() {
        AiGenerationJob active = job(AiGenerationJobType.EXPLANATION, AiTargetType.QT_PASSAGE, 9001L, NOW);
        AiGenerationJob succeeded = job(AiGenerationJobType.EXPLANATION, AiTargetType.QT_PASSAGE, 9002L, NOW);
        succeeded.markRunning(NOW.plusMinutes(1));
        succeeded.markSucceeded(NOW.plusMinutes(2));
        entityManager.flush();
        entityManager.clear();

        assertThat(repository.findActiveGenerationJob(
                AiGenerationJobType.EXPLANATION,
                AiTargetType.QT_PASSAGE,
                9001L
        )).hasValueSatisfying(row -> assertThat(row.id()).isEqualTo(active.getId()));

        assertThat(repository.findActiveGenerationJob(
                AiGenerationJobType.EXPLANATION,
                AiTargetType.QT_PASSAGE,
                9002L
        )).isEmpty();
    }

    private AiGeneratedAsset asset(Long targetId) {
        AiGenerationJob job = job(AiGenerationJobType.EXPLANATION, AiTargetType.BIBLE_VERSE, targetId, NOW);
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
        entityManager.persist(asset);
        entityManager.flush();
        return asset;
    }

    private AiGenerationJob job(
            AiGenerationJobType jobType,
            AiTargetType targetType,
            Long targetId,
            OffsetDateTime createdAt
    ) {
        AiPromptVersion promptVersion = AiPromptVersion.of(
                targetId,
                AiPromptType.EXPLANATION,
                "v" + targetId,
                "hash-" + targetId,
                AiPromptVersionStatus.ACTIVE,
                createdAt
        );
        promptVersion = entityManager.merge(promptVersion);

        AiGenerationJob job = AiGenerationJob.queue(
                jobType,
                targetType,
                targetId,
                promptVersion.getId(),
                createdAt
        );
        entityManager.persist(job);
        return job;
    }

    private void addValidationLog(Long assetId, Long checklistVersionId, OffsetDateTime createdAt) {
        addValidationLog(
                assetId,
                checklistVersionId,
                1,
                AiValidationResult.PASSED,
                AiValidationReviewerType.AUTO,
                createdAt
        );
    }

    private void addValidationLog(
            Long assetId,
            Long checklistVersionId,
            int layer,
            AiValidationResult result,
            AiValidationReviewerType reviewerType,
            OffsetDateTime createdAt
    ) {
        entityManager.persist(AiValidationLog.create(
                assetId,
                null,
                layer,
                result,
                reviewerType,
                checklistVersionId,
                "{}",
                null,
                createdAt
        ));
    }

    private static ListAdminAiAssetsQuery query(Long checklistVersionId) {
        return query(null, checklistVersionId);
    }

    private static ListAdminAiAssetsQuery query(String status, Long checklistVersionId) {
        return new ListAdminAiAssetsQuery(
                1L,
                "ADMIN",
                "REVIEWER",
                null,
                null,
                status,
                null,
                checklistVersionId,
                0,
                20
        );
    }
}
