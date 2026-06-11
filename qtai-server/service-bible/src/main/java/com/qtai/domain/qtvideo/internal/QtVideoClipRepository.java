package com.qtai.domain.qtvideo.internal;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

interface QtVideoClipRepository extends JpaRepository<QtVideoClip, Long> {

    List<QtVideoClip> findByQtPassageIdAndStatusInOrderByApprovedAtDescIdDesc(
            Long qtPassageId,
            Collection<QtVideoClipStatus> statuses);

    Optional<QtVideoClip> findByQtPassageIdAndActiveUniqueKey(Long qtPassageId, String activeUniqueKey);

    boolean existsByQtPassageIdAndStatus(Long qtPassageId, QtVideoClipStatus status);
}
