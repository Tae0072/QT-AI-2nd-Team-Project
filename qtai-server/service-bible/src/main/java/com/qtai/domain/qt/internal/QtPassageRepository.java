package com.qtai.domain.qt.internal;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * QT 본문(qt_passages) 영속성 포트.
 *
 * <p>도메인 경계(MSA 대비, 리뷰 §5.2 #1): bible 소유 테이블({@code bible_books})을 직접 JOIN/조회하지 않는다.
 * 권 메타(testament/code/koreanName/englishName)는 {@link BibleBookLookup}을 통해 bible api로 조회한다.
 */
public interface QtPassageRepository extends JpaRepository<QtPassage, Long> {

    /** 특정 날짜의 QT 본문 조회 (qt_date UNIQUE). */
    Optional<QtPassage> findByQtDate(LocalDate qtDate);

    Optional<QtPassage> findByQtDateAndStatus(LocalDate qtDate, QtPassageStatus status);

    boolean existsByQtDate(LocalDate qtDate);

    /** 절 매핑(qt_passage_verses)이 비어 있는 본문 — 매핑 백필 대상 조회. */
    @Query("""
            select p
              from QtPassage p
             where p.deletedAt is null
               and not exists (
                   select 1
                     from QtPassageVerse v
                    where v.qtPassageId = p.id
               )
            """)
    List<QtPassage> findAllWithoutVerseMappings();

    /**
     * 선택한 본문 범위(book/chapter/verseFrom~verseTo)를 포함하는 QT 본문을 조회한다.
     *
     * <p>성경 목차에서 임의 범위를 조회했을 때, 그 범위를 포괄하는 QT 본문이 있으면
     * 해당 본문의 해설 진입점을 노출하기 위한 매핑이다. startVerse..endVerse가
     * 선택 범위를 포함하는 본문을 startVerse 오름차순으로 반환한다(보통 0~1건).
     */
    @Query("""
            select p
              from QtPassage p
             where p.deletedAt is null
               and p.bookId = :bookId
               and p.chapter = :chapter
               and p.startVerse <= :verseFrom
               and p.endVerse >= :verseTo
             order by p.startVerse asc
            """)
    List<QtPassage> findContainingRange(
            @Param("bookId") Short bookId,
            @Param("chapter") Short chapter,
            @Param("verseFrom") Short verseFrom,
            @Param("verseTo") Short verseTo);
}
