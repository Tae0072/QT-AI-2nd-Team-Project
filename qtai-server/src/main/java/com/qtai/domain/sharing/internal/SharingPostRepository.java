package com.qtai.domain.sharing.internal;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface SharingPostRepository extends JpaRepository<SharingPost, Long> {

    /**
     * 상세 조회용. PUBLISHED 상태만 반환하므로 HIDDEN/DELETED/없는 글은 비어 있고,
     * 호출부에서 404로 처리한다(존재를 숨김, 04 §4.4.2).
     */
    Optional<SharingPost> findByIdAndStatus(Long id, SharingPostStatus status);

    /** 노트 ID로 공유글 조회 — 중복 공유 방지용. DDL note_id UNIQUE 제약과 일치. */
    Optional<SharingPost> findByNoteId(Long noteId);

    /**
     * 나눔 공개(publish) 중복 차단용. 같은 노트가 이미 공개됐는지 검사한다.
     * noteId는 UNIQUE 제약이라 결과는 0건 또는 1건. true면 호출부에서 409로 막는다.
     * (true backstop은 DB UNIQUE 제약 자체이고, 이 메서드는 친절한 에러를 위한 사전 조회다.)
     */
    boolean existsByNoteId(Long noteId);

    /**
     * 나눔 피드 검색. status는 호출부(Service)에서 PUBLISHED를 넘긴다.
     * category가 null이면 전체, q가 null이면 검색어 필터를 건너뛴다.
     * q는 호출부에서 LIKE 와일드카드(%, _, \)를 이스케이프한 값으로 넘기고,
     * 여기서 CONCAT('%', :q, '%')로 감싸 "포함" 검색한다. 정렬은 Pageable이 처리한다.
     */
    @Query("""
            SELECT sp FROM SharingPost sp
            WHERE sp.status = :status
              AND (:category IS NULL OR sp.snapshotCategory = :category)
              AND (:q IS NULL
                   OR sp.snapshotTitle LIKE CONCAT('%', :q, '%') ESCAPE '\\'
                   OR sp.snapshotBody LIKE CONCAT('%', :q, '%') ESCAPE '\\')
            """)
    Page<SharingPost> search(@Param("status") SharingPostStatus status,
                             @Param("category") String category,
                             @Param("q") String q,
                             Pageable pageable);
}
