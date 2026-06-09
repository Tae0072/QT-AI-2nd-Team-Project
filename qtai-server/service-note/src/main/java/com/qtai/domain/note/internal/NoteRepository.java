package com.qtai.domain.note.internal;

import com.qtai.domain.note.api.NoteCategory;
import com.qtai.domain.note.api.NoteStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface NoteRepository extends JpaRepository<Note, Long> {

    @Query("""
            SELECT n FROM Note n
            WHERE n.memberId = :memberId
              AND n.deletedAt IS NULL
              AND (:category IS NULL OR n.category = :category)
              AND (:status IS NULL OR n.status = :status)
              AND (:q IS NULL OR n.title LIKE CONCAT('%', :q, '%') ESCAPE '!'
                              OR n.body LIKE CONCAT('%', :q, '%') ESCAPE '!')
            """)
    Page<Note> search(@Param("memberId") Long memberId,
                      @Param("category") NoteCategory category,
                      @Param("status") NoteStatus status,
                      @Param("q") String q,
                      Pageable pageable);

    @Query("""
            SELECT n FROM Note n
            WHERE n.id = :id
              AND n.memberId = :memberId
              AND n.deletedAt IS NULL
              AND n.status <> com.qtai.domain.note.api.NoteStatus.DELETED
            """)
    Optional<Note> findActiveByIdAndMemberId(@Param("id") Long id, @Param("memberId") Long memberId);

    @Query("""
            SELECT n FROM Note n
            WHERE n.memberId = :memberId
              AND n.category = :category
              AND n.qtPassageId = :qtPassageId
              AND n.status = com.qtai.domain.note.api.NoteStatus.DRAFT
              AND n.deletedAt IS NULL
            """)
    Optional<Note> findDraft(@Param("memberId") Long memberId,
                             @Param("category") NoteCategory category,
                             @Param("qtPassageId") Long qtPassageId);

    boolean existsByMemberIdAndQtPassageIdAndCategoryAndActiveUniqueKey(
            Long memberId,
            Long qtPassageId,
            NoteCategory category,
            String activeUniqueKey
    );

    boolean existsByMemberIdAndQtPassageIdAndCategoryAndActiveUniqueKeyAndIdNot(
            Long memberId,
            Long qtPassageId,
            NoteCategory category,
            String activeUniqueKey,
            Long id
    );

    @Query("""
            SELECT n FROM Note n
            WHERE n.memberId = :memberId
              AND n.status = com.qtai.domain.note.api.NoteStatus.SAVED
              AND n.deletedAt IS NULL
              AND n.savedAt IS NOT NULL
              AND n.savedAt >= :startAt
              AND n.savedAt < :endAt
            ORDER BY n.savedAt ASC, n.id ASC
            """)
    List<Note> findSavedCalendarNotes(@Param("memberId") Long memberId,
                                      @Param("startAt") LocalDateTime startAt,
                                      @Param("endAt") LocalDateTime endAt);

    /**
     * 연속 묵상(streak) 계산용 — 저장된 묵상 노트의 savedAt을 범위로 조회 (P1-9).
     *
     * <p>streak은 월 경계를 넘어 연속으로 인정되므로(2026-06-05 Lead 결정), 달력의 월 범위가
     * 아니라 anchor 이전 충분한 윈도를 조회해 호출자가 일자 집합으로 환산한다.
     */
    @Query("""
            SELECT n.savedAt FROM Note n
            WHERE n.memberId = :memberId
              AND n.status = com.qtai.domain.note.api.NoteStatus.SAVED
              AND n.deletedAt IS NULL
              AND n.savedAt IS NOT NULL
              AND n.savedAt >= :startAt
              AND n.savedAt < :endAt
            """)
    List<LocalDateTime> findSavedMeditationTimestamps(@Param("memberId") Long memberId,
                                                      @Param("startAt") LocalDateTime startAt,
                                                      @Param("endAt") LocalDateTime endAt);
}
