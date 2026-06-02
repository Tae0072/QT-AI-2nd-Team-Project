package com.qtai.domain.ai.internal;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Field;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import com.qtai.config.JpaAuditingConfig;

@DataJpaTest
@ActiveProfiles("test")
@Import({AdminAiBatchRunLogQueryRepository.class, JpaAuditingConfig.class})
class AdminAiBatchRunLogQueryRepositoryTest {

    @Autowired
    private TestEntityManager testEntityManager;

    @Autowired
    private AiBatchRunLogRepository batchRunLogRepository;

    @Autowired
    private AdminAiBatchRunLogQueryRepository repository;

    @Test
    void findAllAppliesFiltersAndCreatedAtDescIdDescPagination() {
        AiBatchRunLog olderMatch = persistLog(
                AiBatchName.AI_DAILY_QT_VERSE_EXPLANATION_SEED,
                AiBatchRunStatus.FAILED,
                "older failed",
                LocalDateTime.parse("2026-06-02T00:05:00")
        );
        AiBatchRunLog firstSameTimeMatch = persistLog(
                AiBatchName.AI_DAILY_QT_VERSE_EXPLANATION_SEED,
                AiBatchRunStatus.FAILED,
                "first same time failed",
                LocalDateTime.parse("2026-06-02T00:06:00")
        );
        AiBatchRunLog secondSameTimeMatch = persistLog(
                AiBatchName.AI_DAILY_QT_VERSE_EXPLANATION_SEED,
                AiBatchRunStatus.FAILED,
                "second same time failed",
                LocalDateTime.parse("2026-06-02T00:06:00")
        );
        persistLog(
                AiBatchName.AI_GENERATION_WORKER_POLL,
                AiBatchRunStatus.FAILED,
                "batch mismatch",
                LocalDateTime.parse("2026-06-02T00:07:00")
        );
        persistLog(
                AiBatchName.AI_DAILY_QT_VERSE_EXPLANATION_SEED,
                AiBatchRunStatus.SUCCEEDED,
                "status mismatch",
                LocalDateTime.parse("2026-06-02T00:08:00")
        );
        persistLog(
                AiBatchName.AI_DAILY_QT_VERSE_EXPLANATION_SEED,
                AiBatchRunStatus.FAILED,
                "date mismatch",
                LocalDateTime.parse("2026-06-04T00:05:00")
        );
        flushAndClear();

        AdminAiBatchRunLogQueryRepository.AdminAiBatchRunLogPage page = repository.findAll(
                new AdminAiBatchRunLogQueryRepository.Filter(
                        AiBatchName.AI_DAILY_QT_VERSE_EXPLANATION_SEED,
                        AiBatchRunStatus.FAILED,
                        LocalDateTime.parse("2026-06-02T00:00:00"),
                        LocalDateTime.parse("2026-06-03T00:00:00")
                ),
                PageRequest.of(0, 2)
        );

        assertThat(page.totalElements()).isEqualTo(3L);
        assertThat(page.content())
                .extracting(AdminAiBatchRunLogQueryRepository.AdminAiBatchRunLogRow::id)
                .containsExactly(secondSameTimeMatch.getId(), firstSameTimeMatch.getId());
        assertThat(page.content().get(0).errorMessage()).isEqualTo("second same time failed");

        AdminAiBatchRunLogQueryRepository.AdminAiBatchRunLogPage secondPage = repository.findAll(
                new AdminAiBatchRunLogQueryRepository.Filter(
                        AiBatchName.AI_DAILY_QT_VERSE_EXPLANATION_SEED,
                        AiBatchRunStatus.FAILED,
                        LocalDateTime.parse("2026-06-02T00:00:00"),
                        LocalDateTime.parse("2026-06-03T00:00:00")
                ),
                PageRequest.of(1, 2)
        );
        assertThat(secondPage.content())
                .extracting(AdminAiBatchRunLogQueryRepository.AdminAiBatchRunLogRow::id)
                .containsExactly(olderMatch.getId());
    }

    @Test
    void findAllAllowsEmptyFilters() {
        AiBatchRunLog failed = persistLog(
                AiBatchName.AI_GENERATION_WORKER_POLL,
                AiBatchRunStatus.FAILED,
                "polling failed",
                LocalDateTime.parse("2026-06-02T00:05:00")
        );
        AiBatchRunLog succeeded = persistLog(
                AiBatchName.AI_DAILY_QT_VERSE_EXPLANATION_SEED,
                AiBatchRunStatus.SUCCEEDED,
                null,
                LocalDateTime.parse("2026-06-02T00:06:00")
        );
        flushAndClear();

        AdminAiBatchRunLogQueryRepository.AdminAiBatchRunLogPage page = repository.findAll(
                new AdminAiBatchRunLogQueryRepository.Filter(null, null, null, null),
                PageRequest.of(0, 10)
        );

        assertThat(page.totalElements()).isEqualTo(2L);
        assertThat(page.content())
                .extracting(AdminAiBatchRunLogQueryRepository.AdminAiBatchRunLogRow::id)
                .containsExactly(succeeded.getId(), failed.getId());
    }

    private AiBatchRunLog persistLog(
            AiBatchName batchName,
            AiBatchRunStatus status,
            String errorMessage,
            LocalDateTime createdAt
    ) {
        OffsetDateTime startedAt = OffsetDateTime.parse("2026-06-02T00:05:00+09:00");
        AiBatchRunLog log = batchRunLogRepository.save(AiBatchRunLog.create(new AiBatchRunLogCommand(
                batchName,
                status,
                status == AiBatchRunStatus.SUCCEEDED ? 1 : 0,
                status == AiBatchRunStatus.PARTIAL_FAILED || status == AiBatchRunStatus.FAILED ? 1 : 0,
                batchName == AiBatchName.AI_GENERATION_WORKER_POLL ? 1 : 0,
                errorMessage == null ? null : "IllegalStateException",
                errorMessage,
                startedAt,
                startedAt.plusSeconds(1)
        )));
        batchRunLogRepository.flush();
        testEntityManager.getEntityManager()
                .createNativeQuery("update ai_batch_run_logs set created_at = ? where id = ?")
                .setParameter(1, Timestamp.valueOf(createdAt))
                .setParameter(2, log.getId())
                .executeUpdate();
        setField(log, "createdAt", createdAt);
        return log;
    }

    private void flushAndClear() {
        testEntityManager.flush();
        testEntityManager.clear();
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
