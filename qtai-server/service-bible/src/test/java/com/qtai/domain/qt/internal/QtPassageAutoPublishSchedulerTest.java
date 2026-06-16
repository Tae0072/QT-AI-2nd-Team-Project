package com.qtai.domain.qt.internal;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class QtPassageAutoPublishSchedulerTest {

    @Mock
    private QtPassageAutoPublishService autoPublishService;

    @Test
    @DisplayName("04:00 scheduled trigger absorbs publish failures")
    void publishAt0400_whenPublishFails_doesNotPropagate() {
        when(autoPublishService.publishDue()).thenThrow(new IllegalStateException("boom"));
        QtPassageAutoPublishScheduler scheduler = new QtPassageAutoPublishScheduler(autoPublishService);

        assertDoesNotThrow(scheduler::publishAt0400);

        verify(autoPublishService).publishDue();
    }

    @Test
    @DisplayName("startup catch-up trigger absorbs publish failures")
    void catchUpOnStartup_whenPublishFails_doesNotPropagate() {
        when(autoPublishService.publishDue()).thenThrow(new IllegalStateException("boom"));
        QtPassageAutoPublishScheduler scheduler = new QtPassageAutoPublishScheduler(autoPublishService);

        assertDoesNotThrow(scheduler::catchUpOnStartup);

        verify(autoPublishService).publishDue();
    }
}
