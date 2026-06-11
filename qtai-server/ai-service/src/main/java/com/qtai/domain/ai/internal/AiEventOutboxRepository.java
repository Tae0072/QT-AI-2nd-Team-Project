package com.qtai.domain.ai.internal;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiEventOutboxRepository extends JpaRepository<AiEventOutbox, Long> {

    Optional<AiEventOutbox> findByEventId(String eventId);

    List<AiEventOutbox> findByStatusOrderByCreatedAtAscIdAsc(AiEventOutboxStatus status, Pageable pageable);
}
