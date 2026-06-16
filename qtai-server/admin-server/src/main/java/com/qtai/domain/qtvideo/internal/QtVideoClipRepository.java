package com.qtai.domain.qtvideo.internal;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

interface QtVideoClipRepository extends JpaRepository<QtVideoClip, Long> {

    Page<QtVideoClip> findByQtPassageId(Long qtPassageId, Pageable pageable);

    Page<QtVideoClip> findByStatus(QtVideoClipStatus status, Pageable pageable);

    Page<QtVideoClip> findByQtPassageIdAndStatus(Long qtPassageId, QtVideoClipStatus status, Pageable pageable);

    Optional<QtVideoClip> findByQtPassageIdAndActiveUniqueKey(Long qtPassageId, String activeUniqueKey);

    long deleteBySourceVideo_Id(Long sourceVideoId);
}
