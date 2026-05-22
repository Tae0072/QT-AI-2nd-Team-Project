package com.qtai.domain.praise.api.dto;

import com.qtai.domain.praise.internal.PraiseSong;

import java.time.LocalDateTime;

/**
 * 찬양 큐레이션 곡 응답 DTO.
 */
public record PraiseResponse(
        Long id,
        String title,
        String artist,
        String sourceType,
        String status,
        LocalDateTime createdAt
) {
    public static PraiseResponse from(PraiseSong song) {
        return new PraiseResponse(
                song.getId(),
                song.getTitle(),
                song.getArtist(),
                song.getSourceType(),
                song.getStatus(),
                song.getCreatedAt()
        );
    }
}
