package com.qtai.domain.ai.internal;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AiValidationChecklistVersionRepository
        extends JpaRepository<AiValidationChecklistVersion, Long> {

    boolean existsByChecklistTypeAndVersion(AiValidationChecklistType checklistType, String version);

    List<AiValidationChecklistVersion> findByChecklistTypeAndStatus(
            AiValidationChecklistType checklistType,
            AiValidationChecklistStatus status
    );

    @Query("""
            select checklistVersion
            from AiValidationChecklistVersion checklistVersion
            where (:checklistType is null or checklistVersion.checklistType = :checklistType)
              and (:status is null or checklistVersion.status = :status)
            """)
    Page<AiValidationChecklistVersion> findAllByFilters(
            @Param("checklistType") AiValidationChecklistType checklistType,
            @Param("status") AiValidationChecklistStatus status,
            Pageable pageable
    );
}
