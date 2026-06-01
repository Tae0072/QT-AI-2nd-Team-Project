package com.qtai.domain.note.internal;

import com.qtai.domain.note.api.JournalChangedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
class JournalEventPersistenceService {

    private final JournalEventRepository journalEventRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public JournalEvent savePending(JournalChangedEvent event) {
        return journalEventRepository.saveAndFlush(JournalEvent.pending(event));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markProcessed(JournalEvent journalEvent, LocalDateTime processedAt) {
        journalEvent.markProcessed(processedAt);
        journalEventRepository.saveAndFlush(journalEvent);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markFailed(JournalEvent journalEvent, String errorMessage, LocalDateTime failedAt) {
        journalEvent.markFailed(errorMessage, failedAt);
        journalEventRepository.saveAndFlush(journalEvent);
    }
}
