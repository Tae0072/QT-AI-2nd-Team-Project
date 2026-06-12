package com.qtai.domain.praise.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 찬양 큐레이션 곡 수정 요청 DTO (ADMIN only).
 *
 * sourceType 은 생성 시 고정(CURATED)이므로 수정 불가.
 * 가사·음원 본문 저장 필드 추가 금지(v3.1 저작권 정책).
 */
public record PraiseUpdateRequest(
        @NotBlank @Size(max = 100) String title,
        @Size(max = 100) String artist,
        @Size(max = 300) String licenseNote,
        // Keep in sync with com.qtai.domain.praise.internal.PraiseSongStatus values exposed to admins.
        @Pattern(regexp = "ACTIVE|HIDDEN", message = "status는 ACTIVE 또는 HIDDEN이어야 합니다")
        String status
) {}
