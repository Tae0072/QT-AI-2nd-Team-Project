package com.qtai.domain.qtvideo.api.dto;

import java.math.BigDecimal;

public record QtVideoClipResponse(
        String status,
        Long clipId,
        Long qtPassageId,
        String title,
        String videoUrl,
        Long sourceVideoId,
        BigDecimal startTimeSec,
        BigDecimal endTimeSec,
        String compositionType,
        String clipStatus
) {
    public static QtVideoClipResponse missing(Long qtPassageId) {
        return new QtVideoClipResponse(
                "MISSING",
                null,
                qtPassageId,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );
    }

    public static QtVideoClipResponse unavailable(
            Long qtPassageId,
            QtVideoUserStatus status,
            String clipStatus) {
        return new QtVideoClipResponse(
                status.name(),
                null,
                qtPassageId,
                null,
                null,
                null,
                null,
                null,
                null,
                clipStatus
        );
    }
}
