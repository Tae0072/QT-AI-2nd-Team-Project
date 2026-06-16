package com.qtai.domain.praise.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 찬양 큐레이션 곡 등록 요청 DTO (ADMIN only).
 *
 * v3.1 게이트: lyricsText, audioUrl 등 본문/음원 직접 저장 필드 추가 금지.
 */
public record PraiseCreateRequest(
        @NotBlank @Size(max = 100) String title,
        @Size(max = 100) String artist,
        @Size(max = 300) String licenseNote,
        // Keep in sync with com.qtai.domain.praise.internal.PraiseSongStatus values exposed to admins.
        @Pattern(regexp = "ACTIVE|HIDDEN", message = "status는 ACTIVE 또는 HIDDEN이어야 합니다")
        String status
) {}
