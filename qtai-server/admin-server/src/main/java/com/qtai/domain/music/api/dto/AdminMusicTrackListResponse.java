package com.qtai.domain.music.api.dto;

import java.util.List;

public record AdminMusicTrackListResponse(
        List<AdminMusicTrackResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last,
        String sort
) {
}
