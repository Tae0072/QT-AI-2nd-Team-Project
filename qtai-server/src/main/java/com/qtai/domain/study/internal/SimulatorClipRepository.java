package com.qtai.domain.study.internal;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

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
}
