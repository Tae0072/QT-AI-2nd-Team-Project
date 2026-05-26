package com.qtai.domain.bible.internal;

/**
 * 성경 영속성 포트. Spring Data JPA로 구현.
 */
public interface BibleRepository {

    // TODO: extends JpaRepository<BibleVerse, Long>
    // TODO: Optional<BibleVerse> findByBookAndChapterAndVerseAndTranslation(String book, int chapter, int verse, String translation);
    // TODO: List<BibleVerse> findByBookAndChapterAndVerseBetweenAndTranslation(String book, int chapter, int start, int end, String translation);
    // TODO: @Query(nativeQuery=true) FULLTEXT MATCH로 키워드 검색 — Page<BibleVerse> 반환
    // TODO: List<String> findDistinctBookBy...(...) / List<Integer> findDistinctChapter...
}
