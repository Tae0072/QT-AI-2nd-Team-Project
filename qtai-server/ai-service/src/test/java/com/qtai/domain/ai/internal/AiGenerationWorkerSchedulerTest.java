package com.qtai.domain.ai.internal;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;

class AiGenerationWorkerSchedulerTest {

    @Test
    void scheduledTickRunsWorkerBatch() {
        AiGenerationWorkerService workerService = mock(AiGenerationWorkerService.class);
        when(workerService.runBatch()).thenReturn(2);
        AiGenerationWorkerScheduler scheduler = new AiGenerationWorkerScheduler(workerService);

        scheduler.runScheduledBatch();

        verify(workerService).runBatch();
    }

    @Test
    void scheduledTickDoesNotPropagateWorkerFailure() {
        AiGenerationWorkerService workerService = mock(AiGenerationWorkerService.class);
        when(workerService.runBatch()).thenThrow(new IllegalStateException("failure details are not propagated"));
        AiGenerationWorkerScheduler scheduler = new AiGenerationWorkerScheduler(workerService);

        assertThatCode(scheduler::runScheduledBatch)
                .doesNotThrowAnyException();
    }
}
