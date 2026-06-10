package com.qtai.domain.qtvideo.internal;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

interface QtVideoClipRepository extends JpaRepository<QtVideoClip, Long> {

    List<QtVideoClip> findByQtPassageIdAndStatusInOrderByApprovedAtDescIdDesc(
            Long qtPassageId,
            Collection<QtVideoClipStatus> statuses);
}
