package com.qtai.domain.note.internal;

import com.qtai.domain.note.api.JournalChangedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
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
    private final Clock clock;

    @Transactional
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(JournalChangedEvent event) {
        if (journalEventRepository.existsByEventId(event.eventId())) {
            return;
        }

        JournalEvent journalEvent = JournalEvent.pending(event);
        try {
            journalEvent.markProcessed(LocalDateTime.now(clock));
            journalEventRepository.save(journalEvent);
        } catch (RuntimeException e) {
            journalEvent.markFailed(e.getMessage(), LocalDateTime.now(clock));
            log.error("journal event handler failed: eventId={}, eventType={}, handlerName={}, errorMessage={}",
                    event.eventId(), event.eventType(), HANDLER_NAME, e.getMessage(), e);
            throw e;
        }
    }
}
