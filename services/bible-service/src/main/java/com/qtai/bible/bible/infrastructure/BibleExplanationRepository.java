package com.qtai.bible.bible.infrastructure;

import com.qtai.bible.bible.domain.BibleExplanation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * 해설 조회.
 *
 * <p>범위 매칭 규칙: 요청 (chapter, verse)이 row의 [chapter_start/verse_start, chapter_end/verse_end] 범위에
 * 포함되면 해당 row를 반환한다. 같은 책 내에서 (chapter, verse)를
 * "장*1000 + 절" 형태의 ordinal로 비교한다고 가정하면 단순하지만, MVP는
 * 같은 chapter 범위 안에서만 verse 범위를 비교하는 패턴이 대부분이라 둘을 모두 커버한다.
 */
public interface BibleExplanationRepository extends JpaRepository<BibleExplanation, Long> {

    /**
     * 사용자에게 노출 가능한 AI 생성 해설만 반환.
     * sourceType = GENERATED_EXPLANATION AND editor_verified_at IS NOT NULL
     * 그리고 (chapter, verse)가 row 범위에 포함.
     */
    @Query("""
           SELECT e FROM BibleExplanation e
            WHERE e.bookId = :bookId
              AND e.sourceType = 'GENERATED_EXPLANATION'
              AND e.editorVerifiedAt IS NOT NULL
              AND (e.chapterStart * 1000 + e.verseStart) <= (:chapter * 1000 + :verse)
              AND (e.chapterEnd   * 1000 + e.verseEnd  ) >= (:chapter * 1000 + :verse)
            ORDER BY e.id
           """)
    List<BibleExplanation> findPublishedForVerse(@Param("bookId") Long bookId,
                                                  @Param("chapter") Integer chapter,
                                                  @Param("verse") Integer verse);

    /**
     * AI 생성 컨텍스트용. REFERENCE_SOURCE row만. 범위 포함되는 모든 원천.
     * AI 응답 컴포저가 이걸로 컨텍스트 적재 후 DeepSeek 호출.
     */
    @Query("""
           SELECT e FROM BibleExplanation e
            WHERE e.bookId = :bookId
              AND e.sourceType = 'REFERENCE_SOURCE'
              AND (e.chapterStart * 1000 + e.verseStart) <= (:chapter * 1000 + :verse)
              AND (e.chapterEnd   * 1000 + e.verseEnd  ) >= (:chapter * 1000 + :verse)
           """)
    List<BibleExplanation> findReferencesForVerse(@Param("bookId") Long bookId,
                                                   @Param("chapter") Integer chapter,
                                                   @Param("verse") Integer verse);
}
