package com.qtai.domain.ai.internal;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AiProcessedEventRepository extends JpaRepository<AiProcessedEvent, Long> {

    boolean existsByEventIdAndHandlerName(String eventId, String handlerName);

    Optional<AiProcessedEvent> findByEventIdAndHandlerName(String eventId, String handlerName);
}
