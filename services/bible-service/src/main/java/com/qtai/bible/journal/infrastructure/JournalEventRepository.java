package com.qtai.bible.journal.infrastructure;

import com.qtai.bible.journal.domain.JournalEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JournalEventRepository extends JpaRepository<JournalEvent, Long> {
    List<JournalEvent> findAllByJournalIdOrderBySequenceAsc(Long journalId);
}
