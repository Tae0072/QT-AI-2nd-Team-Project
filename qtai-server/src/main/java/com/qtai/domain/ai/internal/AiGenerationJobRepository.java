package com.qtai.domain.ai.internal;

import java.util.Collection;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AiGenerationJobRepository extends JpaRepository<AiGenerationJob, Long> {

    boolean existsByJobTypeAndTargetTypeAndTargetIdAndPromptVersionIdAndStatusIn(
            AiGenerationJobType jobType,
            AiTargetType targetType,
            Long targetId,
            Long promptVersionId,
            Collection<AiGenerationJobStatus> statuses
    );
}
