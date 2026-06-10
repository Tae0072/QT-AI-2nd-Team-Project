package com.qtai.domain.study.api.dto;

import java.time.OffsetDateTime;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * 시뮬레이터 클립 게시 커맨드 (P1-11).
 *
 * @param sceneScriptJson 승인본 장면 스크립트 JSON. 서비스에서 파싱 가능 여부·크기를 검증한다.
 */
public record PublishApprovedSimulatorClipCommand(
        @NotNull @Positive Long qtPassageId,
        @NotBlank String title,
        @NotNull @Positive Long componentLibraryVersionId,
        @NotBlank String sceneScriptJson,
        @NotNull @Positive Long aiAssetId,
        @NotNull OffsetDateTime approvedAt
) {
}
