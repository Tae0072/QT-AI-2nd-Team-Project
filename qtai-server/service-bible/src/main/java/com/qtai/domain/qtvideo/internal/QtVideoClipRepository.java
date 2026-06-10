package com.qtai.domain.qtvideo.internal;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

interface QtVideoClipRepository extends JpaRepository<QtVideoClip, Long> {

    Optional<QtVideoClip> findFirstByQtPassageIdAndStatusOrderByApprovedAtDescIdDesc(
            Long qtPassageId,
            QtVideoClipStatus status);
}
