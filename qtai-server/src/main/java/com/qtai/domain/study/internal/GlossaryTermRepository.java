package com.qtai.domain.study.internal;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GlossaryTermRepository extends JpaRepository<GlossaryTerm, Long> {

    List<GlossaryTerm> findByBibleVerseIdInAndStatusOrderByBibleVerseIdAscIdAsc(
            List<Long> bibleVerseIds,
            GlossaryTermStatus status
    );
}
