package com.qtai.domain.note.internal;

import com.qtai.domain.note.api.NoteCategory;
import com.qtai.domain.note.api.NoteStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * 노트 영속성 포트. Spring Data JPA로 구현.
 */
public interface NoteRepository extends JpaRepository<Note, Long> {

    /**
     * 본인 노트 + 삭제 제외 + 선택 필터 동적 조회. (04 API §4.3.1)
     *
     * <p>강제 조건: {@code memberId} 일치, {@code deletedAt IS NULL}.
     * 선택 조건은 nullable 동적 쿼리 표준 관용구 {@code (:param IS NULL OR ...)}로 처리.
     */
    @Query("""
            SELECT n FROM Note n
            WHERE n.memberId = :memberId
              AND n.deletedAt IS NULL
              AND (:category IS NULL OR n.category = :category)
              AND (:status   IS NULL OR n.status   = :status)
              AND (:q IS NULL OR n.title LIKE CONCAT('%', :q, '%')
                              OR n.body  LIKE CONCAT('%', :q, '%'))
            """)
    Page<Note> search(@Param("memberId") Long memberId,
                      @Param("category") NoteCategory category,
                      @Param("status") NoteStatus status,
                      @Param("q") String q,
                      Pageable pageable);
}
