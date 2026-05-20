package com.qtai.domain.report.internal;

/**
 * 신고 엔티티.
 *
 * (reporter_id, share_snapshot_id) UNIQUE — 같은 사람이 같은 대상을 중복 신고 차단.
 * 처리 결과는 status로 추적: PENDING → REVIEWED → DISMISSED/ACTIONED.
 */
// TODO: @Entity, @Table(name = "report",
//        uniqueConstraints = @UniqueConstraint(columnNames = {"reporter_id", "share_snapshot_id"}))
public class Report {

    // TODO: @Id @GeneratedValue Long id;
    // TODO: Long reporterId;
    // TODO: Long shareSnapshotId;
    // TODO: String reason;             — SPAM / HATE / SEXUAL / OTHER
    // TODO: @Column(columnDefinition="TEXT") String detail;
    // TODO: @Enumerated(STRING) ReportStatus status;  — 기본 PENDING
    // TODO: LocalDateTime createdAt;   — @CreationTimestamp
    // TODO: LocalDateTime reviewedAt;
    // TODO: String adminNote;          — 관리자 처리 메모 (nullable)
}
