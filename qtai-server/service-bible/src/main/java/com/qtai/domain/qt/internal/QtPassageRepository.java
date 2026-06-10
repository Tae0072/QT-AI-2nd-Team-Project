package com.qtai.domain.qt.internal;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

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
}
