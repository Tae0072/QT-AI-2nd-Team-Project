package com.qtai.domain.bible.internal;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface BibleRepository extends JpaRepository<BibleVerse, Long> {

    List<BibleVerse> findAllByIdIn(Collection<Long> ids);

    List<BibleVerse> findByBookAndChapterNoOrderByVerseNoAsc(BibleBook book, Short chapterNo);

    List<BibleVerse> findByBookAndChapterNoAndVerseNoBetweenOrderByVerseNoAsc(
            BibleBook book,
            Short chapterNo,
            Short verseFrom,
            Short verseTo
    );
}
