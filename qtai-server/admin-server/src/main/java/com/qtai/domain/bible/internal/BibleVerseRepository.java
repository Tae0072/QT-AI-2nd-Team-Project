package com.qtai.domain.bible.internal;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface BibleVerseRepository extends JpaRepository<BibleVerse, Long> {

    Optional<BibleVerse> findByBook_IdAndChapterNoAndVerseNo(
            Short bookId,
            Short chapterNo,
            Short verseNo
    );
}
