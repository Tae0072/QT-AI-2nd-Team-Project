package com.qtai.domain.qtvideo.internal;

import com.qtai.domain.qt.api.QtPassageVerseMappingsChangedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
class QtVideoClipPreparationListener {

    private static final String STARTUP_HANDLER =
            "QtVideoClipPreparationListener.prepareTodayOnStartup";
    private static final String MAPPING_CHANGED_HANDLER =
            "QtVideoClipPreparationListener.prepareAfterQtVerseMappingsChanged";

    private final QtVideoClipPreparationService preparationService;

    @EventListener(ApplicationReadyEvent.class)
    void prepareTodayOnStartup() {
        try {
            preparationService.prepareToday();
        } catch (RuntimeException exception) {
            log.warn(
                    "Failed to prepare today's QT video clip on startup. eventType={}, handlerName={}, retryable={}, errorType={}, errorMessage={}",
                    ApplicationReadyEvent.class.getSimpleName(),
                    STARTUP_HANDLER,
                    true,
                    exception.getClass().getSimpleName(),
                    exception.getMessage()
            );
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    void prepareAfterQtVerseMappingsChanged(QtPassageVerseMappingsChangedEvent event) {
        try {
            boolean prepared = preparationService.prepare(event.qtPassageId());
            log.info(
                    "Handled QT video clip preparation event. eventId={}, eventType={}, handlerName={}, qtPassageId={}, prepared={}",
                    event.eventId(),
                    event.eventType(),
                    MAPPING_CHANGED_HANDLER,
                    event.qtPassageId(),
                    prepared
            );
        } catch (RuntimeException exception) {
            log.warn(
                    "Failed to prepare QT video clip. eventId={}, eventType={}, handlerName={}, qtPassageId={}, retryable={}, errorType={}, errorMessage={}",
                    event.eventId(),
                    event.eventType(),
                    MAPPING_CHANGED_HANDLER,
                    event.qtPassageId(),
                    true,
                    exception.getClass().getSimpleName(),
                    exception.getMessage()
            );
        }
    }
}
