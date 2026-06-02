package com.qtai.domain.ai.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.scheduling.annotation.Scheduled;

@ExtendWith(OutputCaptureExtension.class)
class AiDailyQtVerseExplanationSeedSchedulerTest {

    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-06-01T15:05:00Z"),
            ZoneId.of("Asia/Seoul")
    );
    private static final OffsetDateTime NOW = OffsetDateTime.parse("2026-06-02T00:05:00+09:00");

    @Test
    void disabledSchedulerDoesNotSeedJobs() {
        AiDailyQtVerseExplanationSeedService service = mock(AiDailyQtVerseExplanationSeedService.class);
        AiBatchMonitoringService monitoringService = mock(AiBatchMonitoringService.class);
        AiDailyQtVerseExplanationSeedScheduler scheduler = scheduler(service, monitoringService, false);

        scheduler.seedDaily();

        verifyNoInteractions(service);
        verifyNoInteractions(monitoringService);
    }

    @Test
    void enabledSchedulerSeedsJobsAndRecordsSucceededBatchRun(CapturedOutput output) {
        AiDailyQtVerseExplanationSeedService service = mock(AiDailyQtVerseExplanationSeedService.class);
        AiBatchMonitoringService monitoringService = mock(AiBatchMonitoringService.class);
        AiDailyQtVerseExplanationSeedScheduler scheduler = scheduler(service, monitoringService, true);
        when(service.seedToday()).thenReturn(new AiDailyQtVerseExplanationSeedResult(2, 0));

        scheduler.seedDaily();

        verify(service).seedToday();
        AiBatchRunLogCommand command = captureMonitoringCommand(monitoringService);
        assertThat(command.batchName()).isEqualTo(AiBatchName.AI_DAILY_QT_VERSE_EXPLANATION_SEED);
        assertThat(command.status()).isEqualTo(AiBatchRunStatus.SUCCEEDED);
        assertThat(command.createdCount()).isEqualTo(2);
        assertThat(command.failedCount()).isZero();
        assertThat(command.processedCount()).isZero();
        assertThat(command.errorType()).isNull();
        assertThat(command.errorMessage()).isNull();
        assertThat(command.startedAt()).isEqualTo(NOW);
        assertThat(command.finishedAt()).isEqualTo(NOW);
        assertThat(output).contains("AI daily QT verse explanation seed completed. createdCount=2, failedCount=0");
    }

    @Test
    void partialFailuresRecordPartialFailedBatchRunAndWarn(CapturedOutput output) {
        AiDailyQtVerseExplanationSeedService service = mock(AiDailyQtVerseExplanationSeedService.class);
        AiBatchMonitoringService monitoringService = mock(AiBatchMonitoringService.class);
        AiDailyQtVerseExplanationSeedScheduler scheduler = scheduler(service, monitoringService, true);
        when(service.seedToday()).thenReturn(new AiDailyQtVerseExplanationSeedResult(2, 1));

        scheduler.seedDaily();

        AiBatchRunLogCommand command = captureMonitoringCommand(monitoringService);
        assertThat(command.status()).isEqualTo(AiBatchRunStatus.PARTIAL_FAILED);
        assertThat(command.createdCount()).isEqualTo(2);
        assertThat(command.failedCount()).isEqualTo(1);
        assertThat(output).contains(
                "AI daily QT verse explanation seed partially failed",
                "createdCount=2",
                "failedCount=1"
        );
    }

    @Test
    void failureReasonRecordsFailedBatchRun(CapturedOutput output) {
        AiDailyQtVerseExplanationSeedService service = mock(AiDailyQtVerseExplanationSeedService.class);
        AiBatchMonitoringService monitoringService = mock(AiBatchMonitoringService.class);
        AiDailyQtVerseExplanationSeedScheduler scheduler = scheduler(service, monitoringService, true);
        when(service.seedToday()).thenReturn(new AiDailyQtVerseExplanationSeedResult(
                0,
                0,
                "ACTIVE_EXPLANATION_PROMPT_VERSION_NOT_FOUND"
        ));

        scheduler.seedDaily();

        AiBatchRunLogCommand command = captureMonitoringCommand(monitoringService);
        assertThat(command.status()).isEqualTo(AiBatchRunStatus.FAILED);
        assertThat(command.errorType()).isEqualTo("ACTIVE_EXPLANATION_PROMPT_VERSION_NOT_FOUND");
        assertThat(command.errorMessage())
                .isEqualTo("AI daily QT verse explanation seed failed: ACTIVE_EXPLANATION_PROMPT_VERSION_NOT_FOUND");
        assertThat(output).contains(
                "AI daily QT verse explanation seed failed",
                "reason=ACTIVE_EXPLANATION_PROMPT_VERSION_NOT_FOUND"
        );
    }

    @Test
    void seedFailureDoesNotPropagateToScheduler(CapturedOutput output) {
        AiDailyQtVerseExplanationSeedService service = mock(AiDailyQtVerseExplanationSeedService.class);
        AiBatchMonitoringService monitoringService = mock(AiBatchMonitoringService.class);
        AiDailyQtVerseExplanationSeedScheduler scheduler = scheduler(service, monitoringService, true);
        doThrow(new IllegalStateException("seed failed"))
                .when(service)
                .seedToday();

        assertThatCode(scheduler::seedDaily)
                .doesNotThrowAnyException();

        verify(service).seedToday();
        AiBatchRunLogCommand command = captureMonitoringCommand(monitoringService);
        assertThat(command.status()).isEqualTo(AiBatchRunStatus.FAILED);
        assertThat(command.errorType()).isEqualTo("IllegalStateException");
        assertThat(command.errorMessage()).isEqualTo("seed failed");
        assertThat(output).contains(
                "AI daily QT verse explanation seed failed",
                "errorType=IllegalStateException",
                "errorMessage=seed failed"
        );
    }

    @Test
    void monitoringFailureDoesNotPropagateToScheduler(CapturedOutput output) {
        AiDailyQtVerseExplanationSeedService service = mock(AiDailyQtVerseExplanationSeedService.class);
        AiBatchMonitoringService monitoringService = mock(AiBatchMonitoringService.class);
        AiDailyQtVerseExplanationSeedScheduler scheduler = scheduler(service, monitoringService, true);
        when(service.seedToday()).thenReturn(new AiDailyQtVerseExplanationSeedResult(1, 0));
        doThrow(new IllegalStateException("monitoring unavailable"))
                .when(monitoringService)
                .record(anyMonitoringCommand());

        assertThatCode(scheduler::seedDaily)
                .doesNotThrowAnyException();

        assertThat(output).contains(
                "AI batch monitoring write failed",
                "batchName=AI_DAILY_QT_VERSE_EXPLANATION_SEED",
                "errorType=IllegalStateException",
                "errorMessage=monitoring unavailable"
        );
    }

    @Test
    void scheduledTriggerUsesLeadApprovedInternalSeedTime() throws NoSuchMethodException {
        Method seedDaily = AiDailyQtVerseExplanationSeedScheduler.class.getDeclaredMethod("seedDaily");
        Scheduled scheduled = seedDaily.getAnnotation(Scheduled.class);

        assertThat(scheduled.cron()).isEqualTo("0 5 0 * * *");
        assertThat(scheduled.zone()).isEqualTo("Asia/Seoul");
    }

    private static AiDailyQtVerseExplanationSeedScheduler scheduler(
            AiDailyQtVerseExplanationSeedService service,
            AiBatchMonitoringService monitoringService,
            boolean enabled
    ) {
        return new AiDailyQtVerseExplanationSeedScheduler(service, monitoringService, enabled, CLOCK);
    }

    private static AiBatchRunLogCommand captureMonitoringCommand(AiBatchMonitoringService monitoringService) {
        ArgumentCaptor<AiBatchRunLogCommand> commandCaptor =
                ArgumentCaptor.forClass(AiBatchRunLogCommand.class);
        verify(monitoringService).record(commandCaptor.capture());
        return commandCaptor.getValue();
    }

    private static AiBatchRunLogCommand anyMonitoringCommand() {
        return org.mockito.ArgumentMatchers.any(AiBatchRunLogCommand.class);
    }
}
