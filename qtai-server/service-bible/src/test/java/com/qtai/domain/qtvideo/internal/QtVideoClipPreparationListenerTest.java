package com.qtai.domain.qtvideo.internal;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.qtai.domain.qt.api.QtPassageVerseMappingsChangedEvent;
import java.lang.reflect.Method;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@ExtendWith(MockitoExtension.class)
class QtVideoClipPreparationListenerTest {

    @Mock
    private QtVideoClipPreparationService preparationService;

    @Test
    @DisplayName("Failure log includes event metadata and retry hint")
    void prepareAfterQtVerseMappingsChanged_failureLogIncludesEventMetadata() {
        when(preparationService.prepare(6L)).thenThrow(new IllegalStateException("boom"));
        QtVideoClipPreparationListener listener = new QtVideoClipPreparationListener(preparationService);
        Logger logger = (Logger) LoggerFactory.getLogger(QtVideoClipPreparationListener.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);

        try {
            listener.prepareAfterQtVerseMappingsChanged(new QtPassageVerseMappingsChangedEvent(
                    "event-1",
                    "QT_PASSAGE_VERSE_MAPPINGS_CHANGED",
                    6L
            ));
        } finally {
            logger.detachAppender(appender);
        }

        verify(preparationService).prepare(6L);
        assertTrue(appender.list.stream().anyMatch(event -> {
            String message = event.getFormattedMessage();
            return event.getLevel() == Level.WARN
                    && message.contains("eventId=event-1")
                    && message.contains("eventType=QT_PASSAGE_VERSE_MAPPINGS_CHANGED")
                    && message.contains("handlerName=QtVideoClipPreparationListener.prepareAfterQtVerseMappingsChanged")
                    && message.contains("qtPassageId=6")
                    && message.contains("retryable=true")
                    && message.contains("errorType=IllegalStateException")
                    && message.contains("errorMessage=boom");
        }));
    }

    @Test
    @DisplayName("Mapping changed event is handled after the publishing transaction commits")
    void prepareAfterQtVerseMappingsChanged_isAfterCommitListener() throws NoSuchMethodException {
        Method method = QtVideoClipPreparationListener.class.getDeclaredMethod(
                "prepareAfterQtVerseMappingsChanged", QtPassageVerseMappingsChangedEvent.class);

        TransactionalEventListener listener = method.getAnnotation(TransactionalEventListener.class);

        org.junit.jupiter.api.Assertions.assertEquals(TransactionPhase.AFTER_COMMIT, listener.phase());
    }
}
