package com.qtai.domain.qtvideo.api.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record AdminQtVideoClipItem(
        Long id,
        Long qtPassageId,
        String title,
        Long sourceVideoId,
        String videoUrl,
        BigDecimal startTimeSec,
        BigDecimal endTimeSec,
        String compositionType,
        String status,
        OffsetDateTime approvedAt
) {
}
