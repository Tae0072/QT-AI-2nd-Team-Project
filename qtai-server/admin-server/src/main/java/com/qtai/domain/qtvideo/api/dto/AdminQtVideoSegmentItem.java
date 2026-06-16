package com.qtai.domain.qtvideo.api.dto;

import java.math.BigDecimal;

public record AdminQtVideoSegmentItem(
        Long id,
        Long bibleVerseId,
        BigDecimal startTimeSec,
        BigDecimal endTimeSec
) {
}
