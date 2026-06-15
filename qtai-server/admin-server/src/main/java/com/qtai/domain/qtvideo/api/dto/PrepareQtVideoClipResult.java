package com.qtai.domain.qtvideo.api.dto;

public record PrepareQtVideoClipResult(
        Long qtPassageId,
        boolean prepared,
        Long clipId
) {
}
