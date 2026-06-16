package com.qtai.domain.qtvideo.api.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record AdminQtVideoSourceItem(
        Long id,
        Short bibleBookId,
        String title,
        String videoUrl,
        BigDecimal durationSec,
        String status,
        OffsetDateTime createdAt
) {
}
