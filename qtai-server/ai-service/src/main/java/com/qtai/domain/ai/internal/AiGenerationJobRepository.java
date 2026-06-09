package com.qtai.domain.ai.internal;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import jakarta.persistence.LockModeType;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface AiGenerationJobRepository extends JpaRepository<AiGenerationJob, Long> {

    boolean existsByJobTypeAndTargetTypeAndTargetIdAndStatusIn(
            AiGenerationJobType jobType,
            AiTargetType targetType,
            Long targetId,
            Collection<AiGenerationJobStatus> statuses
    );

    @Query("""
            select job.id
            from AiGenerationJob job
            where job.status = :status
            order by job.createdAt asc, job.id asc
            """)
    List<Long> findQueuedJobIds(AiGenerationJobStatus status, Pageable pageable);

    /**
     * 사용자 노출용 조회가 아니라 생성/검증 pipeline의 중복 생성 방지용 readiness 조회다.
     */
    @Query("""
            select distinct job.targetId
            from AiGenerationJob job
            where job.jobType = com.qtai.domain.ai.internal.AiGenerationJobType.EXPLANATION
              and job.targetType = com.qtai.domain.ai.internal.AiTargetType.BIBLE_VERSE
              and job.targetId in :targetIds
              and job.status in (
                com.qtai.domain.ai.internal.AiGenerationJobStatus.QUEUED,
                com.qtai.domain.ai.internal.AiGenerationJobStatus.RUNNING
              )
            """)
    List<Long> findActiveExplanationBibleVerseTargetIds(Collection<Long> targetIds);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<AiGenerationJob> findByIdAndStatus(Long id, AiGenerationJobStatus status);

    /**
     * 타임아웃 임계 시각 이전에 시작돼 아직 RUNNING인 고착 job id 조회 (P1-3 스윕).
     *
     * <p>워커가 markRunning(commit) 후 완료 전에 크래시하면 job이 RUNNING으로 영구 고착되고,
     * active_unique_key 때문에 재생성도 막힌다. 이 조회로 찾아 FAILED로 풀어 재처리를 가능케 한다.
     */
    @Query("""
            select job.id
            from AiGenerationJob job
            where job.status = com.qtai.domain.ai.internal.AiGenerationJobStatus.RUNNING
              and job.startedAt is not null
              and job.startedAt < :threshold
            order by job.startedAt asc, job.id asc
            """)
    List<Long> findStaleRunningJobIds(OffsetDateTime threshold, Pageable pageable);
}
