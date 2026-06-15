package com.qtai.domain.music.api.dto;

public record AdminMusicTrackCommand(
        String title,
        String category,
        String mimeType,
        Integer durationSec,
        Integer sortOrder,
        String licenseNote,
        byte[] audioData
) {
}
