package com.qtai.domain.report.api.dto;

/** 신고 생성 요청 DTO. */
public record ReportCreateRequest(
        // TODO: Long shareSnapshotId  — 신고 대상 공유 스냅샷 ID (필수)
        // TODO: String reason         — 신고 카테고리 (SPAM / HATE / SEXUAL / OTHER)
        // TODO: String detail         — 상세 사유 (선택, 길이 제한)
) {}
