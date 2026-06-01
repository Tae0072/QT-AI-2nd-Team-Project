package com.qtai.domain.ai.internal;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Field;
import java.time.OffsetDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
class AiGenerationJobRepositoryTest {

    private static final OffsetDateTime BASE_TIME = OffsetDateTime.parse("2026-05-29T04:00:00+09:00");

    @Autowired
    private TestEntityManager testEntityManager;

    @Autowired
    private AiGenerationJobRepository repository;

    @Test
    void findQueuedJobIdsReturnsQueuedRowsByCreatedAtThenIdWithinBatchSize() {
        AiPromptVersion promptVersion = persistPromptVersion(AiPromptType.EXPLANATION);
        AiGenerationJob first = persistJob(promptVersion, AiGenerationJobStatus.QUEUED, BASE_TIME.minusMinutes(3));
        AiGenerationJob sameTimeFirst = persistJob(promptVersion, AiGenerationJobStatus.QUEUED, BASE_TIME.minusMinutes(1));
        AiGenerationJob sameTimeSecond = persistJob(promptVersion, AiGenerationJobStatus.QUEUED, BASE_TIME.minusMinutes(1));
        persistJob(promptVersion, AiGenerationJobStatus.RUNNING, BASE_TIME.minusMinutes(4));
        persistJob(promptVersion, AiGenerationJobStatus.FAILED, BASE_TIME.minusMinutes(5));
        flushAndClear();

        List<Long> jobIds = repository.findQueuedJobIds(
                AiGenerationJobStatus.QUEUED,
                PageRequest.of(0, 2)
        );

        assertThat(jobIds).containsExactly(first.getId(), sameTimeFirst.getId());

        List<Long> allQueuedIds = repository.findQueuedJobIds(
                AiGenerationJobStatus.QUEUED,
                PageRequest.of(0, 10)
        );
        assertThat(allQueuedIds).containsExactly(first.getId(), sameTimeFirst.getId(), sameTimeSecond.getId());
    }

    @Test
    void findByIdAndStatusClaimsOnlyMatchingQueuedRow() {
        AiPromptVersion promptVersion = persistPromptVersion(AiPromptType.EXPLANATION);
        AiGenerationJob queued = persistJob(promptVersion, AiGenerationJobStatus.QUEUED, BASE_TIME);
        AiGenerationJob running = persistJob(promptVersion, AiGenerationJobStatus.RUNNING, BASE_TIME.plusMinutes(1));
        AiGenerationJob succeeded = persistJob(promptVersion, AiGenerationJobStatus.SUCCEEDED, BASE_TIME.plusMinutes(2));
        AiGenerationJob failed = persistJob(promptVersion, AiGenerationJobStatus.FAILED, BASE_TIME.plusMinutes(3));
        flushAndClear();

        assertThat(repository.findByIdAndStatus(queued.getId(), AiGenerationJobStatus.QUEUED)).isPresent();
        assertThat(repository.findByIdAndStatus(running.getId(), AiGenerationJobStatus.QUEUED)).isEmpty();
        assertThat(repository.findByIdAndStatus(succeeded.getId(), AiGenerationJobStatus.QUEUED)).isEmpty();
        assertThat(repository.findByIdAndStatus(failed.getId(), AiGenerationJobStatus.QUEUED)).isEmpty();
    }

    @Test
    void findActiveExplanationBibleVerseTargetIdsFiltersActiveVerseExplanationJobs() {
        AiPromptVersion explanationPrompt = persistPromptVersion(AiPromptType.EXPLANATION);
        AiPromptVersion simulatorPrompt = persistPromptVersion(AiPromptType.SIMULATOR);
        persistJob(explanationPrompt, AiGenerationJobStatus.QUEUED, BASE_TIME, AiGenerationJobType.EXPLANATION,
                AiTargetType.BIBLE_VERSE, 101L);
        persistJob(explanationPrompt, AiGenerationJobStatus.RUNNING, BASE_TIME.plusMinutes(1),
                AiGenerationJobType.EXPLANATION, AiTargetType.BIBLE_VERSE, 102L);
        persistJob(explanationPrompt, AiGenerationJobStatus.SUCCEEDED, BASE_TIME.plusMinutes(2),
                AiGenerationJobType.EXPLANATION, AiTargetType.BIBLE_VERSE, 103L);
        persistJob(explanationPrompt, AiGenerationJobStatus.FAILED, BASE_TIME.plusMinutes(3),
                AiGenerationJobType.EXPLANATION, AiTargetType.BIBLE_VERSE, 104L);
        persistJob(explanationPrompt, AiGenerationJobStatus.QUEUED, BASE_TIME.plusMinutes(4),
                AiGenerationJobType.EXPLANATION, AiTargetType.QT_PASSAGE, 105L);
        persistJob(simulatorPrompt, AiGenerationJobStatus.QUEUED, BASE_TIME.plusMinutes(5),
                AiGenerationJobType.SIMULATOR, AiTargetType.BIBLE_VERSE, 106L);
        flushAndClear();

        List<Long> targetIds = repository.findActiveExplanationBibleVerseTargetIds(
                List.of(101L, 102L, 103L, 104L, 105L, 106L));

        assertThat(targetIds).containsExactlyInAnyOrder(101L, 102L);
    }

    private AiPromptVersion persistPromptVersion(AiPromptType promptType) {
        AiPromptVersion promptVersion = new AiPromptVersion();
        setField(promptVersion, "promptType", promptType);
        setField(promptVersion, "version", "2026.05." + promptType);
        setField(promptVersion, "contentHash", "hash-" + promptType);
        setField(promptVersion, "status", AiPromptVersionStatus.ACTIVE);
        setField(promptVersion, "createdAt", BASE_TIME.minusDays(1));
        return testEntityManager.persistAndFlush(promptVersion);
    }

    private AiGenerationJob persistJob(
            AiPromptVersion promptVersion,
            AiGenerationJobStatus status,
            OffsetDateTime createdAt
    ) {
        return persistJob(
                promptVersion,
                status,
                createdAt,
                AiGenerationJobType.EXPLANATION,
                AiTargetType.QT_PASSAGE,
                nextTargetId()
        );
    }

    private AiGenerationJob persistJob(
            AiPromptVersion promptVersion,
            AiGenerationJobStatus status,
            OffsetDateTime createdAt,
            AiGenerationJobType jobType,
            AiTargetType targetType,
            Long targetId
    ) {
        AiGenerationJob job = AiGenerationJob.queue(
                jobType,
                targetType,
                targetId,
                promptVersion.getId(),
                createdAt
        );
        if (status == AiGenerationJobStatus.RUNNING) {
            job.markRunning(createdAt.plusSeconds(1));
        } else if (status == AiGenerationJobStatus.SUCCEEDED) {
            job.markRunning(createdAt.plusSeconds(1));
            job.markSucceeded(createdAt.plusSeconds(2));
        } else if (status == AiGenerationJobStatus.FAILED) {
            job.markFailed("TEST_FAILURE", createdAt.plusSeconds(2));
        }
        return testEntityManager.persistAndFlush(job);
    }

    private void flushAndClear() {
        testEntityManager.flush();
        testEntityManager.clear();
    }

    private long nextTargetId = 100L;

    private Long nextTargetId() {
        return nextTargetId++;
    }

    private static void setField(Object target, String fieldName, Object value) {
        try {
            Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
