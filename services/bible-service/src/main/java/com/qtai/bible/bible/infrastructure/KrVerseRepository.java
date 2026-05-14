package com.qtai.bible.bible.infrastructure;

import com.qtai.bible.bible.domain.KrVerse;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface KrVerseRepository extends JpaRepository<KrVerse, Long> {
    Optional<KrVerse> findFirstByBookIdAndChapterAndVerseAndVersion(Long bookId, Integer chapter, Integer verse, String version);
}
