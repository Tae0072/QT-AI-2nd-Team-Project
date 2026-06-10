package com.qtai.domain.study.internal;

import java.util.List;
import java.util.Optional;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SimulatorClipRepository extends JpaRepository<SimulatorClip, Long> {

    Optional<SimulatorClip> findFirstByQtPassageIdAndStatusOrderByApprovedAtDescIdDesc(
            Long qtPassageId,
            SimulatorClipStatus status
    );

    Optional<SimulatorClip> findByIdAndQtPassageIdAndStatus(
            Long id,
            Long qtPassageId,
            SimulatorClipStatus status
    );

    /** 숨김 처리용 — 해당 AI 산출물로 노출 중인 APPROVED 클립을 잠그고 조회 (P1-11). */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select c from SimulatorClip c
             where c.aiAssetId = :aiAssetId
               and c.status = com.qtai.domain.study.internal.SimulatorClipStatus.APPROVED
            """)
    List<SimulatorClip> findApprovedByAiAssetIdForUpdate(@Param("aiAssetId") Long aiAssetId);
}
