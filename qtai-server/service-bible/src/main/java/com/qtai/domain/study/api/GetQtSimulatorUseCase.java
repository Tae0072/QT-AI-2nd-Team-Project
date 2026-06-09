package com.qtai.domain.study.api;

import com.qtai.domain.study.api.dto.QtSimulatorResponse;

public interface GetQtSimulatorUseCase {

    QtSimulatorResponse getSimulator(Long qtPassageId);

    QtSimulatorResponse getSimulatorClip(Long qtPassageId, Long clipId);
}
