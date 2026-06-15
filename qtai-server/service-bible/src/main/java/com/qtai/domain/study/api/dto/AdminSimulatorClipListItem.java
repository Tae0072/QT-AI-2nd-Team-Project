package com.qtai.domain.study.api.dto;

import java.time.OffsetDateTime;

/**
 * 관리자 시뮬레이터 클립 목록 항목 (F-06/F-12). 원문(sceneScriptJson)은 목록에 포함하지 않는다.
 */
public record AdminSimulatorClipListItem(
        Long id,
        Long qtPassageId,
        String title,
        String status,
        Long aiAssetId,
        OffsetDateTime approvedAt
) {
}
