package com.qtai.domain.note.internal;

import com.qtai.domain.note.api.JournalChangedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.Clock;
import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class JournalEventHandler {

    static final String HANDLER_NAME = "JournalEventHandler";

    private final JournalEventRepository journalEventRepository;
    private final JournalEventPersistenceService journalEventPersistenceService;
    private final Clock clock;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(JournalChangedEvent event) {
        if (journalEventRepository.existsByEventId(event.eventId())) {
            return;
        }

        JournalEvent journalEvent;
        try {
            journalEvent = journalEventPersistenceService.savePending(event);
        } catch (DataIntegrityViolationException e) {
            if (journalEventRepository.existsByEventId(event.eventId())) {
                return;
            }
            throw e;
        }

        try {
            journalEventPersistenceService.markProcessed(journalEvent, LocalDateTime.now(clock));
        } catch (RuntimeException e) {
            markFailed(event, journalEvent, e);
            log.error("journal event handler failed: eventId={}, eventType={}, handlerName={}, errorMessage={}",
                    event.eventId(), event.eventType(), HANDLER_NAME, e.getMessage(), e);
            throw e;
        }
    }

    private void markFailed(JournalChangedEvent event, JournalEvent journalEvent, RuntimeException cause) {
        try {
            journalEventPersistenceService.markFailed(journalEvent, cause.getMessage(), LocalDateTime.now(clock));
        } catch (RuntimeException saveFailure) {
            cause.addSuppressed(saveFailure);
            log.error("journal event failure state save failed: eventId={}, eventType={}, handlerName={}, errorMessage={}",
                    event.eventId(), event.eventType(), HANDLER_NAME, saveFailure.getMessage(), saveFailure);
        }
    }
}
