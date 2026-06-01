package com.qtai.domain.ai.internal;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface AiGeneratedAssetRepository extends JpaRepository<AiGeneratedAsset, Long> {

    @Query("""
            select distinct asset.targetId
            from AiGeneratedAsset asset
            where asset.assetType = :assetType
              and asset.targetType = :targetType
              and asset.targetId in :targetIds
              and asset.status in :statuses
            """)
    List<Long> findTargetIdsByAssetTypeAndTargetTypeAndTargetIdInAndStatusIn(
            AiGeneratedAssetType assetType,
            AiTargetType targetType,
            Collection<Long> targetIds,
            Collection<AiGeneratedAssetStatus> statuses
    );
}
