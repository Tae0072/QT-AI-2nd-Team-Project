package com.qtai.domain.music.api.dto;

/**
 * 배경음악 스트리밍 바이트 응답 DTO.
 *
 * @param data 음원 바이트
 * @param mimeType 콘텐츠 타입(예: audio/mpeg)
 * @param byteSize 바이트 크기
 */
public record MusicTrackAudioResponse(
        byte[] data,
        String mimeType,
        Long byteSize
) {
}
