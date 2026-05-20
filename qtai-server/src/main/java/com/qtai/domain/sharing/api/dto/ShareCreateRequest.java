package com.qtai.domain.sharing.api.dto;

/** 공유 생성 요청 DTO. */
public record ShareCreateRequest(
        // TODO: String resourceType   — QT / NOTE / STUDY (필수)
        // TODO: Long resourceId       — 공유할 원본 ID (필수)
        // TODO: LocalDateTime expiresAt — 만료 시각 (선택, null이면 무기한)
) {}
