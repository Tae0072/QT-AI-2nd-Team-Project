package com.qtai.journal.service;

import com.qtai.journal.domain.Journal;
import com.qtai.journal.event.JournalCreatedEvent;
import com.qtai.journal.repository.JournalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class JournalService {

    private final JournalRepository journalRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public Journal save(Journal journal) {
        Journal saved = journalRepository.save(journal);
        eventPublisher.publishEvent(new JournalCreatedEvent(saved.getId(), saved.getUserId()));
        return saved;
    }
}
