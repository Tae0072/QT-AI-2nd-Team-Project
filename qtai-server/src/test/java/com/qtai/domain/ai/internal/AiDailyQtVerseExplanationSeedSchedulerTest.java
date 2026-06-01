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
class AiDailyQtVerseExplanationSeedSchedulerTest {

    @Test
    void disabledSchedulerDoesNotSeedJobs() {
        AiDailyQtVerseExplanationSeedService service = mock(AiDailyQtVerseExplanationSeedService.class);
        AiDailyQtVerseExplanationSeedScheduler scheduler = new AiDailyQtVerseExplanationSeedScheduler(service, false);

        scheduler.seedDaily();

        verifyNoInteractions(service);
    }

    @Test
    void enabledSchedulerSeedsJobs(CapturedOutput output) {
        AiDailyQtVerseExplanationSeedService service = mock(AiDailyQtVerseExplanationSeedService.class);
        AiDailyQtVerseExplanationSeedScheduler scheduler = new AiDailyQtVerseExplanationSeedScheduler(service, true);
        when(service.seedToday()).thenReturn(2);

        scheduler.seedDaily();

        verify(service).seedToday();
        assertThat(output).contains("AI daily QT verse explanation seed completed. createdCount=2");
    }

    @Test
    void seedFailureDoesNotPropagateToScheduler(CapturedOutput output) {
        AiDailyQtVerseExplanationSeedService service = mock(AiDailyQtVerseExplanationSeedService.class);
        AiDailyQtVerseExplanationSeedScheduler scheduler = new AiDailyQtVerseExplanationSeedScheduler(service, true);
        doThrow(new IllegalStateException("seed failed"))
                .when(service)
                .seedToday();

        assertThatCode(scheduler::seedDaily)
                .doesNotThrowAnyException();

        verify(service).seedToday();
        assertThat(output).contains(
                "AI daily QT verse explanation seed failed",
                "errorType=IllegalStateException",
                "errorMessage=seed failed"
        );
    }
}
