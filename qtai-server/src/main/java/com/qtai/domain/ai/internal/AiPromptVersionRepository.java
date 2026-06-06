package com.qtai.domain.ai.internal;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AiPromptVersionRepository extends JpaRepository<AiPromptVersion, Long> {

    Optional<AiPromptVersion> findFirstByPromptTypeAndStatusOrderByCreatedAtDescIdDesc(
            AiPromptType promptType,
            AiPromptVersionStatus status
    );
}
