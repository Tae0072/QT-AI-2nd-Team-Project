package com.qtai.domain.study.internal;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

import jakarta.persistence.LockModeType;

public interface GlossaryTermRepository extends JpaRepository<GlossaryTerm, Long> {

    List<GlossaryTerm> findByBibleVerseIdInAndStatusOrderByBibleVerseIdAscIdAsc(
            List<Long> bibleVerseIds,
            GlossaryTermStatus status
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select glossaryTerm
            from GlossaryTerm glossaryTerm
            where glossaryTerm.bibleVerseId in :bibleVerseIds
              and glossaryTerm.status = com.qtai.domain.study.internal.GlossaryTermStatus.APPROVED
            """)
    List<GlossaryTerm> findApprovedByBibleVerseIdInForUpdate(
            @Param("bibleVerseIds") List<Long> bibleVerseIds
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select glossaryTerm
            from GlossaryTerm glossaryTerm
            where glossaryTerm.aiAssetId = :aiAssetId
              and glossaryTerm.status = com.qtai.domain.study.internal.GlossaryTermStatus.APPROVED
            """)
    List<GlossaryTerm> findApprovedByAiAssetIdForUpdate(@Param("aiAssetId") Long aiAssetId);
}
