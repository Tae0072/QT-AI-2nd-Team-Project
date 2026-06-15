package com.qtai.domain.music.api.dto;

import java.time.LocalDateTime;

public record AdminMusicTrackResponse(
        Long id,
        String title,
        String category,
        String mimeType,
        Long byteSize,
        Integer durationSec,
        Integer sortOrder,
        String licenseNote,
        String status,
        String streamUrl,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
