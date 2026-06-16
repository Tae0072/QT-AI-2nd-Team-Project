package com.qtai.domain.qtvideo.internal;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

interface QtVideoClipRepository extends JpaRepository<QtVideoClip, Long> {

    // 소프트 삭제(deleted_at) 정책: 목록 조회는 삭제되지 않은 행만 노출한다.
    Page<QtVideoClip> findByDeletedAtIsNull(Pageable pageable);

    Page<QtVideoClip> findByQtPassageIdAndDeletedAtIsNull(Long qtPassageId, Pageable pageable);

    Page<QtVideoClip> findByStatusAndDeletedAtIsNull(QtVideoClipStatus status, Pageable pageable);

    Page<QtVideoClip> findByQtPassageIdAndStatusAndDeletedAtIsNull(
            Long qtPassageId, QtVideoClipStatus status, Pageable pageable);

    Optional<QtVideoClip> findByQtPassageIdAndActiveUniqueKey(Long qtPassageId, String activeUniqueKey);

    // 원본 영상 소프트 삭제 시 동반 소프트 삭제할 클립을 적재한다.
    List<QtVideoClip> findBySourceVideo_IdAndDeletedAtIsNull(Long sourceVideoId);
}
