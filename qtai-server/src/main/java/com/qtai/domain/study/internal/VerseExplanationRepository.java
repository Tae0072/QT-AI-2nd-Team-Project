package com.qtai.domain.study.internal;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

import jakarta.persistence.LockModeType;

public interface VerseExplanationRepository extends JpaRepository<VerseExplanation, Long> {

    List<VerseExplanation> findByBibleVerseIdInAndStatusAndActiveUniqueKey(
            List<Long> bibleVerseIds,
            VerseExplanationStatus status,
            String activeUniqueKey
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select explanation
            from VerseExplanation explanation
            where explanation.bibleVerseId = :bibleVerseId
              and explanation.status = com.qtai.domain.study.internal.VerseExplanationStatus.APPROVED
              and explanation.activeUniqueKey = 'ACTIVE'
            """)
    List<VerseExplanation> findActiveApprovedByBibleVerseIdForUpdate(@Param("bibleVerseId") Long bibleVerseId);
}
