package com.qtai.domain.ai.internal;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface AiGeneratedAssetRepository extends JpaRepository<AiGeneratedAsset, Long> {

    /**
     * 사용자 노출용 조회가 아니라 검증 pipeline에서 추가 생성 대상인지 판별하는 readiness 조회다.
     */
    @Query("""
            select distinct asset.targetId
            from AiGeneratedAsset asset
            where asset.assetType = com.qtai.domain.ai.internal.AiGeneratedAssetType.EXPLANATION
              and asset.targetType = com.qtai.domain.ai.internal.AiTargetType.BIBLE_VERSE
              and asset.targetId in :targetIds
              and asset.status in (
                com.qtai.domain.ai.internal.AiGeneratedAssetStatus.VALIDATING,
                com.qtai.domain.ai.internal.AiGeneratedAssetStatus.APPROVED
              )
            """)
    List<Long> findReadyExplanationBibleVerseTargetIds(Collection<Long> targetIds);
}
