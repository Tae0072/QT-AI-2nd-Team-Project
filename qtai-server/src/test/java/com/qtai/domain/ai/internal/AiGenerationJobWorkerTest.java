package com.qtai.domain.ai.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

@ExtendWith(OutputCaptureExtension.class)
class AiGenerationJobWorkerTest {

    @Test
    void disabledWorkerDoesNotPollQueuedJobs() {
        AiGenerationJobRunner runner = mock(AiGenerationJobRunner.class);
        AiGenerationJobWorker worker = new AiGenerationJobWorker(runner, false, 5);

        worker.pollQueuedJobs();

        verifyNoInteractions(runner);
    }

    @Test
    void enabledWorkerPollsQueuedJobsWithConfiguredBatchSize() {
        AiGenerationJobRunner runner = mock(AiGenerationJobRunner.class);
        AiGenerationJobWorker worker = new AiGenerationJobWorker(runner, true, 7);

        worker.pollQueuedJobs();

        verify(runner).runQueuedBatch(7);
    }

    @Test
    void pollingFailureDoesNotPropagateToScheduler(CapturedOutput output) {
        AiGenerationJobRunner runner = mock(AiGenerationJobRunner.class);
        AiGenerationJobWorker worker = new AiGenerationJobWorker(runner, true, 5);
        doThrow(new IllegalStateException("polling failed"))
                .when(runner)
                .runQueuedBatch(5);

        assertThatCode(worker::pollQueuedJobs)
                .doesNotThrowAnyException();

        verify(runner).runQueuedBatch(5);
        assertThat(output).contains(
                "AI generation worker polling failed",
                "errorType=IllegalStateException",
                "errorMessage=polling failed"
        );
    }

    @Test
    void processedJobsAreLogged(CapturedOutput output) {
        AiGenerationJobRunner runner = mock(AiGenerationJobRunner.class);
        AiGenerationJobWorker worker = new AiGenerationJobWorker(runner, true, 5);
        when(runner.runQueuedBatch(5)).thenReturn(3);

        worker.pollQueuedJobs();

        verify(runner).runQueuedBatch(5);
        assertThat(output).contains("AI generation worker processed jobs. processedCount=3");
    }

    @Test
    void zeroProcessedJobsAreNotLogged(CapturedOutput output) {
        AiGenerationJobRunner runner = mock(AiGenerationJobRunner.class);
        AiGenerationJobWorker worker = new AiGenerationJobWorker(runner, true, 5);
        when(runner.runQueuedBatch(5)).thenReturn(0);

        worker.pollQueuedJobs();

        verify(runner).runQueuedBatch(5);
        assertThat(output).doesNotContain("AI generation worker processed jobs");
    }
}
