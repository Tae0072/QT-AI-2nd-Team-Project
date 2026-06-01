package com.qtai.domain.ai.internal;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
class AiGeneratedAssetRepositoryTest {

    private static final OffsetDateTime BASE_TIME = OffsetDateTime.parse("2026-06-01T00:05:00+09:00");

    @Autowired
    private TestEntityManager testEntityManager;

    @Autowired
    private AiGeneratedAssetRepository repository;

    @Test
    void findTargetIdsByAssetTypeAndTargetTypeAndTargetIdInAndStatusInFiltersVerseExplanationAssets() {
        persistAsset(AiGeneratedAssetType.EXPLANATION, AiTargetType.BIBLE_VERSE, 101L, AiGeneratedAssetStatus.VALIDATING);
        persistAsset(AiGeneratedAssetType.EXPLANATION, AiTargetType.BIBLE_VERSE, 102L, AiGeneratedAssetStatus.APPROVED);
        persistAsset(AiGeneratedAssetType.EXPLANATION, AiTargetType.BIBLE_VERSE, 103L, AiGeneratedAssetStatus.REJECTED);
        persistAsset(AiGeneratedAssetType.EXPLANATION, AiTargetType.QT_PASSAGE, 104L, AiGeneratedAssetStatus.VALIDATING);
        persistAsset(AiGeneratedAssetType.SIMULATOR, AiTargetType.BIBLE_VERSE, 105L, AiGeneratedAssetStatus.VALIDATING);
        flushAndClear();

        List<Long> targetIds = repository.findTargetIdsByAssetTypeAndTargetTypeAndTargetIdInAndStatusIn(
                AiGeneratedAssetType.EXPLANATION,
                AiTargetType.BIBLE_VERSE,
                List.of(101L, 102L, 103L, 104L, 105L),
                List.of(AiGeneratedAssetStatus.VALIDATING, AiGeneratedAssetStatus.APPROVED)
        );

        assertThat(targetIds).containsExactlyInAnyOrder(101L, 102L);
    }

    private void persistAsset(
            AiGeneratedAssetType assetType,
            AiTargetType targetType,
            Long targetId,
            AiGeneratedAssetStatus status
    ) {
        AiGeneratedAsset asset = AiGeneratedAsset.create(
                nextGenerationJobId(),
                assetType,
                targetType,
                targetId,
                "{}",
                "test-source",
                BASE_TIME
        );
        if (status == AiGeneratedAssetStatus.APPROVED) {
            asset.approve(BASE_TIME.plusMinutes(1));
        } else if (status == AiGeneratedAssetStatus.REJECTED) {
            asset.reject(BASE_TIME.plusMinutes(1));
        } else if (status == AiGeneratedAssetStatus.HIDDEN) {
            asset.hide(BASE_TIME.plusMinutes(1));
        }
        testEntityManager.persistAndFlush(asset);
    }

    private void flushAndClear() {
        testEntityManager.flush();
        testEntityManager.clear();
    }

    private long nextGenerationJobId = 1_000L;

    private Long nextGenerationJobId() {
        return nextGenerationJobId++;
    }
}
