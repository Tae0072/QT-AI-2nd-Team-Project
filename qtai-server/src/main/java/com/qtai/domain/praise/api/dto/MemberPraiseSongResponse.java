package com.qtai.domain.praise.api.dto;

import com.qtai.domain.praise.internal.MemberPraiseSong;

import java.time.LocalDateTime;

/**
 * 내 찬양 목록 응답 DTO.
 *
 * API 명세서 §4.6.4 기준.
 */
public record MemberPraiseSongResponse(
        Long id,
        Long praiseSongId,
        String displayTitle,
        String title,
        String artist,
        String sourceType,
        String deviceSongKey,
        LocalDateTime createdAt
) {
    /** 큐레이션 곡 정보가 있는 경우. */
    public static MemberPraiseSongResponse of(MemberPraiseSong mps,
                                               String songTitle,
                                               String songArtist,
                                               String sourceType) {
        return new MemberPraiseSongResponse(
                mps.getId(),
                mps.getPraiseSongId(),
                mps.getDisplayTitle(),
                songTitle,
                songArtist,
                sourceType,
                mps.getDeviceSongKey(),
                mps.getCreatedAt()
        );
    }

    /** 디바이스 전용 (큐레이션 곡 없음). */
    public static MemberPraiseSongResponse fromDevice(MemberPraiseSong mps) {
        return new MemberPraiseSongResponse(
                mps.getId(),
                null,
                mps.getDisplayTitle(),
                null,
                null,
                "DEVICE",
                mps.getDeviceSongKey(),
                mps.getCreatedAt()
        );
    }
}
