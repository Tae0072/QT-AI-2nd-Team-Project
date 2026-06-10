package com.qtai.domain.study.api.dto;

public record PublishApprovedSimulatorClipResult(
        Long simulatorClipId,
        Long qtPassageId,
        String status
) {
}
