package com.qtai.domain.ai.internal;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AiValidationLogRepository extends JpaRepository<AiValidationLog, Long> {

    Optional<AiValidationLog> findFirstByAiAssetIdAndChecklistVersionIdOrderByCreatedAtDescIdDesc(
            Long aiAssetId,
            Long checklistVersionId
    );

    Optional<AiValidationLog> findFirstByAiAssetIdAndLayerAndReviewerTypeOrderByCreatedAtDescIdDesc(
            Long aiAssetId,
            Integer layer,
            AiValidationReviewerType reviewerType
    );
}
