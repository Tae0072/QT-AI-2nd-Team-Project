package com.qtai.domain.note.internal;

import com.qtai.domain.note.internal.event.JournalChangedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class JournalEventHandler {

    private static final String HANDLER_NAME = "JournalEventHandler";

    private final JournalEventRepository journalEventRepository;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(JournalChangedEvent event) {
        try {
            saveIfAbsent(event);
        } catch (DataIntegrityViolationException e) {
            if (journalEventRepository.existsByEventId(event.eventId())) {
                return;
            }
            logFailure(event, e);
        } catch (Exception e) {
            logFailure(event, e);
        }
    }

    private void saveIfAbsent(JournalChangedEvent event) {
        if (journalEventRepository.existsByEventId(event.eventId())) {
            return;
        }
        journalEventRepository.save(JournalEvent.from(event));
    }

    private void logFailure(JournalChangedEvent event, Exception e) {
        log.warn(
                "journal event handler failed: eventId={}, eventType={}, handler={}, error={}",
                event.eventId(),
                event.eventType(),
                HANDLER_NAME,
                e.getMessage()
        );
    }
}
