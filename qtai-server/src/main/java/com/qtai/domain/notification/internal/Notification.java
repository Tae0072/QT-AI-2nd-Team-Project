package com.qtai.domain.notification.internal;

/**
 * 알림 엔티티.
 *
 * 수신자별 (recipientId, createdAt) 복합 인덱스 권장 — 알림 센터는 최신순 조회가 잦다.
 */
// TODO: @Entity, @Table(name = "notification",
//        indexes = @Index(columnList = "recipient_id, created_at"))
public class Notification {

    // TODO: @Id @GeneratedValue Long id;
    // TODO: Long recipientId;
    // TODO: String type;              — PRAISE / COMMENT / MISSION_DONE ...
    // TODO: String title;
    // TODO: @Column(columnDefinition="TEXT") String body;
    // TODO: String linkUrl;           — nullable
    // TODO: boolean read;             — 기본 false
    // TODO: LocalDateTime createdAt;  — @CreationTimestamp
    // TODO: LocalDateTime readAt;     — 읽음 처리 시각 (nullable)
}
