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
        AiGenerationJob job = AiGenerationJob.queue(
                AiGenerationJobType.EXPLANATION,
                AiTargetType.QT_PASSAGE,
                nextTargetId(),
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
