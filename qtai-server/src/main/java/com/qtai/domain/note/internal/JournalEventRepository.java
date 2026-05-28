package com.qtai.domain.note.internal;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JournalEventRepository extends JpaRepository<JournalEvent, Long> {

    boolean existsByEventId(UUID eventId);

    Optional<JournalEvent> findByEventId(UUID eventId);

    List<JournalEvent> findByStatusOrderByOccurredAtAsc(JournalEventStatus status);
}
