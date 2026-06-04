package com.qtai.domain.qt.internal;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Optional;

/**
 * QT 본문(qt_passages) 영속성 포트.
 */
public interface QtPassageRepository extends JpaRepository<QtPassage, Long> {

    /** 특정 날짜의 QT 본문 조회 (qt_date UNIQUE). */
    Optional<QtPassage> findByQtDate(LocalDate qtDate);

    boolean existsByQtDate(LocalDate qtDate);

    @Query(value = """
            SELECT b.id
              FROM bible_books b
             WHERE b.english_name = :englishName
            """, nativeQuery = true)
    Optional<Short> findBookIdByEnglishName(@Param("englishName") String englishName);

    @Query(value = """
            SELECT b.testament AS testament,
                   b.code AS bookCode,
                   b.korean_name AS koreanBookName,
                   b.english_name AS englishBookName,
                   p.chapter AS chapter,
                   p.start_verse AS verseFrom,
                   p.end_verse AS verseTo
              FROM qt_passages p
              JOIN bible_books b ON b.id = p.book_id
             WHERE p.id = :qtPassageId
            """, nativeQuery = true)
    Optional<QtPassageRangeView> findRangeByQtPassageId(@Param("qtPassageId") Long qtPassageId);
}
