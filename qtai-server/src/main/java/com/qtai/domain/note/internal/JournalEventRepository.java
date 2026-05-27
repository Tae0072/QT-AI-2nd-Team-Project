package com.qtai.domain.note.internal;

import org.springframework.data.jpa.repository.JpaRepository;

public interface JournalEventRepository extends JpaRepository<JournalEvent, Long> {

    boolean existsByEventId(String eventId);
}
