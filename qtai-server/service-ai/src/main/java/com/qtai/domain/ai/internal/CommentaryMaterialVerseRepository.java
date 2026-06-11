package com.qtai.domain.ai.internal;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CommentaryMaterialVerseRepository extends JpaRepository<CommentaryMaterialVerse, Long> {

    @Query("""
            select mapping
            from CommentaryMaterialVerse mapping
            join fetch mapping.material material
            join fetch material.source source
            where mapping.bibleVerseId in :verseIds
              and material.status = com.qtai.domain.ai.internal.CommentaryMaterialStatus.ACTIVE
              and source.status = com.qtai.domain.ai.internal.CommentarySourceStatus.ACTIVE
              and source.usageType = 'GENERATION_INPUT'
            order by mapping.bibleVerseId asc, mapping.displayOrder asc, material.id asc
            """)
    List<CommentaryMaterialVerse> findActiveGenerationMappingsByVerseIds(@Param("verseIds") List<Long> verseIds);
}
