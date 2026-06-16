package com.qtai.domain.ai.internal;

import java.time.Clock;
import java.time.OffsetDateTime;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
class AiDailyQtVerseExplanationSeedScheduler {

    private final AiDailyQtVerseExplanationSeedService service;
    private final AiBatchMonitoringService monitoringService;
    private final boolean enabled;
    private final Clock clock;

    AiDailyQtVerseExplanationSeedScheduler(
            AiDailyQtVerseExplanationSeedService service,
            AiBatchMonitoringService monitoringService,
            @Value("${ai.daily-qt-verse-seed.enabled:false}") boolean enabled,
            Clock clock
    ) {
        this.service = service;
        this.monitoringService = monitoringService;
        this.enabled = enabled;
        this.clock = clock;
    }

    @Scheduled(cron = "0 5 0 * * *", zone = "Asia/Seoul")
    void seedDaily() {
        if (!enabled) {
            return;
        }
        OffsetDateTime startedAt = now();
        try {
            AiDailyQtVerseExplanationSeedResult result = service.seedToday();
            OffsetDateTime finishedAt = now();
            AiBatchRunStatus status = seedStatus(result);
            recordBatchRun(seedCommand(result, status, startedAt, finishedAt));
            logSeedResult(result, status);
        } catch (RuntimeException exception) {
            OffsetDateTime finishedAt = now();
            recordBatchRun(failedCommand(exception, startedAt, finishedAt));
            log.warn(
                    "AI daily QT verse explanation seed failed. errorType={}, errorMessage={}",
                    exception.getClass().getSimpleName(),
                    exception.getMessage()
            );
        }
    }

    private void logSeedResult(AiDailyQtVerseExplanationSeedResult result, AiBatchRunStatus status) {
        if (status == AiBatchRunStatus.FAILED) {
            log.warn(
                    "AI daily QT verse explanation seed failed. reason={}",
                    result.failureReason()
            );
            return;
        }
        if (status == AiBatchRunStatus.PARTIAL_FAILED) {
            log.warn(
                    "AI daily QT verse explanation seed partially failed. createdCount={}, failedCount={}",
                    result.createdCount(),
                    result.failedCount()
            );
            return;
        }
        log.info(
                "AI daily QT verse explanation seed completed. createdCount={}, failedCount={}",
                result.createdCount(),
                result.failedCount()
        );
    }

    private void recordBatchRun(AiBatchRunLogCommand command) {
        try {
            monitoringService.record(command);
        } catch (RuntimeException exception) {
            log.warn(
                    "AI batch monitoring write failed. batchName={}, errorType={}, errorMessage={}",
                    command.batchName(),
                    exception.getClass().getSimpleName(),
                    exception.getMessage()
            );
        }
    }

    private static AiBatchRunLogCommand seedCommand(
            AiDailyQtVerseExplanationSeedResult result,
            AiBatchRunStatus status,
            OffsetDateTime startedAt,
            OffsetDateTime finishedAt
    ) {
        return new AiBatchRunLogCommand(
                AiBatchName.AI_DAILY_QT_VERSE_EXPLANATION_SEED,
                status,
                result.createdCount(),
                result.failedCount(),
                0,
                result.failureReason(),
                result.hasFailureReason() ? "AI daily QT verse explanation seed failed: " + result.failureReason() : null,
                startedAt,
                finishedAt
        );
    }

    private static AiBatchRunLogCommand failedCommand(
            RuntimeException exception,
            OffsetDateTime startedAt,
            OffsetDateTime finishedAt
    ) {
        return new AiBatchRunLogCommand(
                AiBatchName.AI_DAILY_QT_VERSE_EXPLANATION_SEED,
                AiBatchRunStatus.FAILED,
                0,
                0,
                0,
                exception.getClass().getSimpleName(),
                exception.getMessage(),
                startedAt,
                finishedAt
        );
    }

    private static AiBatchRunStatus seedStatus(AiDailyQtVerseExplanationSeedResult result) {
        if (result.hasFailureReason()) {
            return AiBatchRunStatus.FAILED;
        }
        if (result.failedCount() > 0) {
            return AiBatchRunStatus.PARTIAL_FAILED;
        }
        return AiBatchRunStatus.SUCCEEDED;
    }

    private OffsetDateTime now() {
        return OffsetDateTime.now(clock);
    }
}
