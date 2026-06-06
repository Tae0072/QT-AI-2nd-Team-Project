package com.qtai.domain.ai.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

@ExtendWith(OutputCaptureExtension.class)
class AiGenerationJobWorkerTest {

    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-06-01T15:10:00Z"),
            ZoneId.of("Asia/Seoul")
    );
    private static final OffsetDateTime NOW = OffsetDateTime.parse("2026-06-02T00:10:00+09:00");

    @Test
    void disabledWorkerDoesNotPollQueuedJobs() {
        AiGenerationJobRunner runner = mock(AiGenerationJobRunner.class);
        AiBatchMonitoringService monitoringService = mock(AiBatchMonitoringService.class);
        AiGenerationJobWorker worker = worker(runner, monitoringService, false, 5);

        worker.pollQueuedJobs();

        verifyNoInteractions(runner);
        verifyNoInteractions(monitoringService);
    }

    @Test
    void enabledWorkerPollsQueuedJobsWithConfiguredBatchSize() {
        AiGenerationJobRunner runner = mock(AiGenerationJobRunner.class);
        AiBatchMonitoringService monitoringService = mock(AiBatchMonitoringService.class);
        AiGenerationJobWorker worker = worker(runner, monitoringService, true, 7);

        worker.pollQueuedJobs();

        verify(runner).runQueuedBatch(7);
        verifyNoInteractions(monitoringService);
    }

    @Test
    void pollingFailureDoesNotPropagateToScheduler(CapturedOutput output) {
        AiGenerationJobRunner runner = mock(AiGenerationJobRunner.class);
        AiBatchMonitoringService monitoringService = mock(AiBatchMonitoringService.class);
        AiGenerationJobWorker worker = worker(runner, monitoringService, true, 5);
        doThrow(new IllegalStateException("polling failed"))
                .when(runner)
                .runQueuedBatch(5);

        assertThatCode(worker::pollQueuedJobs)
                .doesNotThrowAnyException();

        verify(runner).runQueuedBatch(5);
        AiBatchRunLogCommand command = captureMonitoringCommand(monitoringService);
        assertThat(command.batchName()).isEqualTo(AiBatchName.AI_GENERATION_WORKER_POLL);
        assertThat(command.status()).isEqualTo(AiBatchRunStatus.FAILED);
        assertThat(command.createdCount()).isZero();
        assertThat(command.failedCount()).isZero();
        assertThat(command.processedCount()).isZero();
        assertThat(command.errorType()).isEqualTo("IllegalStateException");
        assertThat(command.errorMessage()).isEqualTo("polling failed");
        assertThat(command.startedAt()).isEqualTo(NOW);
        assertThat(command.finishedAt()).isEqualTo(NOW);
        assertThat(output).contains(
                "AI generation worker polling failed",
                "errorType=IllegalStateException",
                "errorMessage=polling failed"
        );
    }

    @Test
    void monitoringFailureDoesNotPropagateToWorker(CapturedOutput output) {
        AiGenerationJobRunner runner = mock(AiGenerationJobRunner.class);
        AiBatchMonitoringService monitoringService = mock(AiBatchMonitoringService.class);
        AiGenerationJobWorker worker = worker(runner, monitoringService, true, 5);
        doThrow(new IllegalStateException("polling failed"))
                .when(runner)
                .runQueuedBatch(5);
        doThrow(new IllegalStateException("monitoring unavailable"))
                .when(monitoringService)
                .record(anyMonitoringCommand());

        assertThatCode(worker::pollQueuedJobs)
                .doesNotThrowAnyException();

        assertThat(output).contains(
                "AI batch monitoring write failed",
                "batchName=AI_GENERATION_WORKER_POLL",
                "errorType=IllegalStateException",
                "errorMessage=monitoring unavailable"
        );
    }

    @Test
    void processedJobsAreLogged(CapturedOutput output) {
        AiGenerationJobRunner runner = mock(AiGenerationJobRunner.class);
        AiBatchMonitoringService monitoringService = mock(AiBatchMonitoringService.class);
        AiGenerationJobWorker worker = worker(runner, monitoringService, true, 5);
        when(runner.runQueuedBatch(5)).thenReturn(3);

        worker.pollQueuedJobs();

        verify(runner).runQueuedBatch(5);
        verifyNoInteractions(monitoringService);
        assertThat(output).contains("AI generation worker processed jobs. processedCount=3");
    }

    @Test
    void zeroProcessedJobsAreNotLogged(CapturedOutput output) {
        AiGenerationJobRunner runner = mock(AiGenerationJobRunner.class);
        AiBatchMonitoringService monitoringService = mock(AiBatchMonitoringService.class);
        AiGenerationJobWorker worker = worker(runner, monitoringService, true, 5);
        when(runner.runQueuedBatch(5)).thenReturn(0);

        worker.pollQueuedJobs();

        verify(runner).runQueuedBatch(5);
        verifyNoInteractions(monitoringService);
        assertThat(output).doesNotContain("AI generation worker processed jobs");
    }

    private static AiGenerationJobWorker worker(
            AiGenerationJobRunner runner,
            AiBatchMonitoringService monitoringService,
            boolean enabled,
            int batchSize
    ) {
        return new AiGenerationJobWorker(runner, monitoringService, enabled, batchSize, 300000L, CLOCK);
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
