package com.qtai.domain.ai.internal;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import org.junit.jupiter.api.Test;

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
    void pollingFailureDoesNotPropagateToScheduler() {
        AiGenerationJobRunner runner = mock(AiGenerationJobRunner.class);
        AiGenerationJobWorker worker = new AiGenerationJobWorker(runner, true, 5);
        doThrow(new IllegalStateException("polling failed"))
                .when(runner)
                .runQueuedBatch(5);

        assertThatCode(worker::pollQueuedJobs)
                .doesNotThrowAnyException();

        verify(runner).runQueuedBatch(5);
    }
}
