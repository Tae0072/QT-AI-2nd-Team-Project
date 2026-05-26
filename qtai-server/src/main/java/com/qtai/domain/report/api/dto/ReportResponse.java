package com.qtai.domain.report.api.dto;

/** 신고 응답 DTO. */
public record ReportResponse(
        // TODO: Long id
        // TODO: Long reporterId           — 신고자
        // TODO: Long shareSnapshotId      — 신고 대상
        // TODO: String reason
        // TODO: String detail
        // TODO: String status             — PENDING / REVIEWED / DISMISSED / ACTIONED
        // TODO: LocalDateTime createdAt
        // TODO: LocalDateTime reviewedAt  — 관리자가 처리한 시각 (nullable)
) {}
