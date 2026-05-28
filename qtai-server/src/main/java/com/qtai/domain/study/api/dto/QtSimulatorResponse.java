package com.qtai.domain.study.api.dto;

import com.fasterxml.jackson.databind.JsonNode;

public record QtSimulatorResponse(
        String status,
        Long clipId,
        Long qtPassageId,
        String title,
        String componentLibraryVersion,
        JsonNode sceneScriptJson,
        String clipStatus
) {
    public static QtSimulatorResponse missing(Long qtPassageId) {
        return new QtSimulatorResponse(
                QtSimulatorUserStatus.MISSING.name(),
                null,
                qtPassageId,
                null,
                null,
                null,
                null
        );
    }

    public static QtSimulatorResponse failed(Long qtPassageId) {
        return new QtSimulatorResponse(
                QtSimulatorUserStatus.FAILED.name(),
                null,
                qtPassageId,
                null,
                null,
                null,
                null
        );
    }
}
