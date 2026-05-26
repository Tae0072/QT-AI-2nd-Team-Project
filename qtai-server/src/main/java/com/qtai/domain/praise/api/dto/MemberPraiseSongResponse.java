package com.qtai.domain.praise.api.dto;

import java.time.LocalDateTime;

/**
 * 내 찬양 목록 응답 DTO.
 *
 * <p>API 명세서 §4.6.4 기준.
 * <p>도메인 경계 정책: api/dto 는 internal 패키지를 import 하지 않는다.
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
}
