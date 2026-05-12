package com.qtai.journal.service;

import com.qtai.journal.domain.Journal;
import com.qtai.journal.event.JournalCreatedEvent;
import com.qtai.journal.repository.JournalRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.context.ApplicationEventPublisher;

@Slf4j
@Service
@RequiredArgsConstructor
public class JournalService {

    private final JournalRepository journalRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public Journal save(Journal journal) {
        if (journal == null) {
            throw new IllegalArgumentException("Journal must not be null");
        }
        Journal saved = journalRepository.save(journal);
        log.info("Journal created: id={}, userId={}", saved.getId(), saved.getUserId());
        eventPublisher.publishEvent(new JournalCreatedEvent(saved.getId(), saved.getUserId()));
        return saved;
    }
}
