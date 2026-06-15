package com.qtai.domain.ai.internal;

import java.util.Optional;
import java.util.List;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AiPromptVersionRepository extends JpaRepository<AiPromptVersion, Long> {

    Optional<AiPromptVersion> findFirstByPromptTypeAndStatusOrderByCreatedAtDescIdDesc(
            AiPromptType promptType,
            AiPromptVersionStatus status
    );

    boolean existsByPromptTypeAndVersion(AiPromptType promptType, String version);

    Page<AiPromptVersion> findByPromptType(AiPromptType promptType, Pageable pageable);

    Page<AiPromptVersion> findByStatus(AiPromptVersionStatus status, Pageable pageable);

    Page<AiPromptVersion> findByPromptTypeAndStatus(
            AiPromptType promptType,
            AiPromptVersionStatus status,
            Pageable pageable
    );

    @Query("""
            select promptVersion.promptType
            from AiPromptVersion promptVersion
            where promptVersion.id = :id
            """)
    Optional<AiPromptType> findPromptTypeById(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select promptVersion
            from AiPromptVersion promptVersion
            where promptVersion.promptType = :promptType
            order by promptVersion.id asc
            """)
    List<AiPromptVersion> findAllByPromptTypeForUpdate(@Param("promptType") AiPromptType promptType);
}
