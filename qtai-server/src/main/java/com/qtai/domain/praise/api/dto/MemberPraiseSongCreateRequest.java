package com.qtai.domain.praise.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 내 찬양 저장 요청 DTO.
 *
 * API 명세서 §4.6.4 기준.
 * praiseSongId OR deviceSongKey 중 하나 이상 필수.
 */
public record MemberPraiseSongCreateRequest(
        Long praiseSongId,
        @Size(max = 200) String deviceSongKey,
        @NotBlank @Size(max = 100) String displayTitle
) {}
