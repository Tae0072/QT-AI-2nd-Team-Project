package com.qtai.domain.study.internal;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VerseExplanationRepository extends JpaRepository<VerseExplanation, Long> {

    List<VerseExplanation> findByBibleVerseIdInAndStatusAndActiveUniqueKey(
            List<Long> bibleVerseIds,
            VerseExplanationStatus status,
            String activeUniqueKey
    );
}
