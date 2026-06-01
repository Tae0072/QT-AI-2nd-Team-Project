package com.qtai.domain.ai.internal;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import jakarta.persistence.LockModeType;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface AiGenerationJobRepository extends JpaRepository<AiGenerationJob, Long> {

    boolean existsByJobTypeAndTargetTypeAndTargetIdAndPromptVersionIdAndStatusIn(
            AiGenerationJobType jobType,
            AiTargetType targetType,
            Long targetId,
            Long promptVersionId,
            Collection<AiGenerationJobStatus> statuses
    );

    @Query("""
            select job.id
            from AiGenerationJob job
            where job.status = :status
            order by job.createdAt asc, job.id asc
            """)
    List<Long> findQueuedJobIds(AiGenerationJobStatus status, Pageable pageable);

    @Query("""
            select distinct job.targetId
            from AiGenerationJob job
            where job.jobType = :jobType
              and job.targetType = :targetType
              and job.targetId in :targetIds
              and job.status in :statuses
            """)
    List<Long> findTargetIdsByJobTypeAndTargetTypeAndTargetIdInAndStatusIn(
            AiGenerationJobType jobType,
            AiTargetType targetType,
            Collection<Long> targetIds,
            Collection<AiGenerationJobStatus> statuses
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<AiGenerationJob> findByIdAndStatus(Long id, AiGenerationJobStatus status);
}
