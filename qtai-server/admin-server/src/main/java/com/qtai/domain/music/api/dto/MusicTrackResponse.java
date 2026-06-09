package com.qtai.domain.music.api.dto;

/**
 * 배경음악 목록 항목 응답 DTO (메타데이터 + 스트리밍 URL).
 *
 * <p>도메인 경계 정책: api/dto 는 internal 패키지를 import 하지 않는다(category 는 String).
 */
public record MusicTrackResponse(
        Long id,
        String title,
        String category,
        String mimeType,
        Long byteSize,
        Integer durationSec,
        Integer sortOrder,
        String streamUrl
) {
}
