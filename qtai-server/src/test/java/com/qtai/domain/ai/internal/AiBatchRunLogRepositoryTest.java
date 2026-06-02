package com.qtai.domain.ai.internal;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import com.qtai.config.JpaAuditingConfig;

@DataJpaTest
@ActiveProfiles("test")
@Import(JpaAuditingConfig.class)
class AiBatchRunLogRepositoryTest {

    @Autowired
    private AiBatchRunLogRepository repository;

    @Test
    void savesBatchRunLogAndFindsLatestByBatchName() {
        OffsetDateTime firstStartedAt = OffsetDateTime.parse("2026-06-02T00:05:00+09:00");
        OffsetDateTime secondStartedAt = OffsetDateTime.parse("2026-06-02T00:10:00+09:00");
        AiBatchRunLog first = repository.save(AiBatchRunLog.create(new AiBatchRunLogCommand(
                AiBatchName.AI_DAILY_QT_VERSE_EXPLANATION_SEED,
                AiBatchRunStatus.SUCCEEDED,
                3,
                0,
                0,
                null,
                null,
                firstStartedAt,
                firstStartedAt
        )));
        AiBatchRunLog second = repository.save(AiBatchRunLog.create(new AiBatchRunLogCommand(
                AiBatchName.AI_DAILY_QT_VERSE_EXPLANATION_SEED,
                AiBatchRunStatus.PARTIAL_FAILED,
                2,
                1,
                0,
                null,
                null,
                secondStartedAt,
                secondStartedAt
        )));
        repository.flush();

        assertThat(first.getCreatedAt()).isNotNull();
        assertThat(first.getCreatedAt()).isNotEqualTo(first.getFinishedAt().toLocalDateTime());
        repository.save(AiBatchRunLog.create(new AiBatchRunLogCommand(
                AiBatchName.AI_GENERATION_WORKER_POLL,
                AiBatchRunStatus.FAILED,
                0,
                0,
                0,
                "IllegalStateException",
                "polling failed",
                secondStartedAt,
                secondStartedAt
        )));

        assertThat(repository.findByBatchNameOrderByCreatedAtDescIdDesc(
                AiBatchName.AI_DAILY_QT_VERSE_EXPLANATION_SEED,
                PageRequest.of(0, 2)
        ))
                .extracting(AiBatchRunLog::getId)
                .containsExactly(second.getId(), first.getId());
    }

    @Test
    void findsBatchRunLogsByStatus() {
        OffsetDateTime now = OffsetDateTime.parse("2026-06-02T00:05:00+09:00");
        AiBatchRunLog failed = repository.save(AiBatchRunLog.create(new AiBatchRunLogCommand(
                AiBatchName.AI_GENERATION_WORKER_POLL,
                AiBatchRunStatus.FAILED,
                0,
                0,
                0,
                "IllegalStateException",
                "polling failed",
                now,
                now
        )));
        repository.save(AiBatchRunLog.create(new AiBatchRunLogCommand(
                AiBatchName.AI_DAILY_QT_VERSE_EXPLANATION_SEED,
                AiBatchRunStatus.SUCCEEDED,
                1,
                0,
                0,
                null,
                null,
                now,
                now
        )));

        assertThat(repository.findByStatusOrderByCreatedAtDescIdDesc(
                AiBatchRunStatus.FAILED,
                PageRequest.of(0, 10)
        ))
                .extracting(AiBatchRunLog::getId)
                .containsExactly(failed.getId());
    }
}
