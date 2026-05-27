package com.qtai.domain.ai.internal;

import java.util.List;
import java.util.Optional;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AiValidationChecklistVersionRepository
        extends JpaRepository<AiValidationChecklistVersion, Long> {

    boolean existsByChecklistTypeAndVersion(AiValidationChecklistType checklistType, String version);

    List<AiValidationChecklistVersion> findByChecklistTypeAndStatus(
            AiValidationChecklistType checklistType,
            AiValidationChecklistStatus status
    );

    Page<AiValidationChecklistVersion> findByChecklistType(
            AiValidationChecklistType checklistType,
            Pageable pageable
    );

    Page<AiValidationChecklistVersion> findByStatus(
            AiValidationChecklistStatus status,
            Pageable pageable
    );

    Page<AiValidationChecklistVersion> findByChecklistTypeAndStatus(
            AiValidationChecklistType checklistType,
            AiValidationChecklistStatus status,
            Pageable pageable
    );

    @Query("""
            select checklistVersion.checklistType
            from AiValidationChecklistVersion checklistVersion
            where checklistVersion.id = :id
            """)
    Optional<AiValidationChecklistType> findChecklistTypeById(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select checklistVersion
            from AiValidationChecklistVersion checklistVersion
            where checklistVersion.checklistType = :checklistType
            order by checklistVersion.id asc
            """)
    List<AiValidationChecklistVersion> findAllByChecklistTypeForUpdate(
            @Param("checklistType") AiValidationChecklistType checklistType
    );
}
