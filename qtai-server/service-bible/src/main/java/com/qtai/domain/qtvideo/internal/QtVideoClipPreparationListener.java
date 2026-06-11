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

    private final QtVideoClipPreparationService preparationService;

    @EventListener(ApplicationReadyEvent.class)
    void prepareTodayOnStartup() {
        try {
            preparationService.prepareToday();
        } catch (RuntimeException exception) {
            log.warn(
                    "Failed to prepare today's QT video clip on startup. errorType={}, errorMessage={}",
                    exception.getClass().getSimpleName(),
                    exception.getMessage()
            );
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    void prepareAfterQtVerseMappingsChanged(QtPassageVerseMappingsChangedEvent event) {
        try {
            preparationService.prepare(event.qtPassageId());
        } catch (RuntimeException exception) {
            log.warn(
                    "Failed to prepare QT video clip. qtPassageId={}, errorType={}, errorMessage={}",
                    event.qtPassageId(),
                    exception.getClass().getSimpleName(),
                    exception.getMessage()
            );
        }
    }
}
