package com.qtai.ai.infrastructure.persistence;

import com.qtai.ai.domain.AiSession;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiSessionRepository extends JpaRepository<AiSession, Long> {
    Page<AiSession> findAllByUserIdAndStatusOrderByCreatedAtDesc(Long userId, String status, Pageable pageable);
}
