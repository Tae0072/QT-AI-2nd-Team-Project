package com.qtai.domain.qtvideo.internal;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

interface SourceVideoRepository extends JpaRepository<SourceVideo, Long> {

    // 소프트 삭제(deleted_at) 정책: 목록 조회는 삭제되지 않은 행만 노출한다.
    Page<SourceVideo> findByDeletedAtIsNull(Pageable pageable);

    Page<SourceVideo> findByBibleBookIdAndDeletedAtIsNull(Short bibleBookId, Pageable pageable);

    Page<SourceVideo> findByStatusAndDeletedAtIsNull(SourceVideoStatus status, Pageable pageable);

    Page<SourceVideo> findByBibleBookIdAndStatusAndDeletedAtIsNull(
            Short bibleBookId, SourceVideoStatus status, Pageable pageable);

    Optional<SourceVideo> findByBibleBookIdAndActiveUniqueKey(Short bibleBookId, String activeUniqueKey);
}
