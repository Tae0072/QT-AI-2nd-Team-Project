package com.qtai.domain.sharing.api.dto;

/** 공유 스냅샷 응답 DTO. */
public record SharedSnapshotResponse(
        // TODO: Long snapshotId
        // TODO: String shareToken         — 공유 URL에 들어가는 토큰
        // TODO: String resourceType       — QT / NOTE / STUDY
        // TODO: String snapshotJson       — 원본 시점 데이터 (JSON 직렬화)
        // TODO: String ownerNickname      — 공유 만든 사람 (개인 식별 최소화)
        // TODO: LocalDateTime sharedAt
        // TODO: LocalDateTime expiresAt   — null이면 무기한
        // TODO: boolean revoked
) {}
