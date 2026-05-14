package com.qtai.ai.infrastructure.persistence;

import com.qtai.ai.domain.AiTurn;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AiTurnRepository extends JpaRepository<AiTurn, Long> {
    List<AiTurn> findAllBySessionIdOrderByCreatedAtAsc(Long sessionId);
}
