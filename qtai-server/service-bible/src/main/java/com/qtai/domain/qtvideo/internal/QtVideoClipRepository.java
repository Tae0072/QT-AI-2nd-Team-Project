package com.qtai.domain.qtvideo.internal;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

interface QtVideoClipRepository extends JpaRepository<QtVideoClip, Long> {

    // 소프트 삭제(deleted_at) 클립은 사용자 노출 대상에서 제외한다(관리자 삭제 정책과 일치).
    List<QtVideoClip> findByQtPassageIdAndStatusInAndDeletedAtIsNullOrderByApprovedAtDescIdDesc(
            Long qtPassageId,
            Collection<QtVideoClipStatus> statuses);

    Optional<QtVideoClip> findByQtPassageIdAndActiveUniqueKey(Long qtPassageId, String activeUniqueKey);

    boolean existsByQtPassageIdAndStatus(Long qtPassageId, QtVideoClipStatus status);

    // [임시 2026-06-19] 오늘 QT 영상 미생성 한시적 폴백용 — 가장 최근 등록(APPROVED, 미삭제) 클립 1건. 원복 시 제거.
    Optional<QtVideoClip> findTopByStatusAndDeletedAtIsNullOrderByApprovedAtDescIdDesc(QtVideoClipStatus status);
}
