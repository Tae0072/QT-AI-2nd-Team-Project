package com.qtai.bible.bible.infrastructure;

import com.qtai.bible.bible.domain.EnVerse;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EnVerseRepository extends JpaRepository<EnVerse, Long> {
    Optional<EnVerse> findFirstByBookIdAndChapterAndVerseAndVersion(Long bookId, Integer chapter, Integer verse, String version);
}
