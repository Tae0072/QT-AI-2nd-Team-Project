package com.qtai.domain.notification.internal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 알림 엔티티.
 *
 * ERD: notifications 테이블 (V6).
 * 수신자별 (member_id, read_at, created_at) 복합 인덱스 — 알림 센터는 최신순 조회가 잦다.
 * event_key UNIQUE로 동일 이벤트 중복 알림 방지.
 */
@Entity
@Table(name = "notifications",
        indexes = @Index(name = "idx_notifications_member_read_created",
                columnList = "member_id, read_at, created_at"),
        uniqueConstraints = @UniqueConstraint(
                name = "uk_notifications_member_event",
                columnNames = {"member_id", "event_key"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 수신 회원 ID (members.id FK — Long FK, 도메인 경계 준수). */
    @Column(name = "member_id", nullable = false)
    private Long memberId;

    /** 알림 유형: LIKE, COMMENT, REPORT_RESULT, NOTICE. */
    @Column(name = "type", nullable = false, length = 30)
    private String type;

    @Column(name = "title", nullable = false, length = 100)
    private String title;

    @Column(name = "body", length = 500)
    private String body;

    /** 공지 알림인 경우 notices.id (nullable). */
    @Column(name = "notice_id")
    private Long noticeId;

    /** 이동 대상 타입: SHARING_POST, REPORT 등. */
    @Column(name = "link_type", length = 30)
    private String linkType;

    /** 이동 대상 ID. */
    @Column(name = "link_id")
    private Long linkId;

    /** 동일 이벤트 중복 알림 방지 키. */
    @Column(name = "event_key", length = 120)
    private String eventKey;

    /** 읽음 시각 (null이면 미읽음). */
    @Column(name = "read_at")
    private LocalDateTime readAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    public Notification(Long memberId, String type, String title, String body,
                        Long noticeId, String linkType, Long linkId, String eventKey) {
        this.memberId = memberId;
        this.type = type;
        this.title = title;
        this.body = body;
        this.noticeId = noticeId;
        this.linkType = linkType;
        this.linkId = linkId;
        this.eventKey = eventKey;
        this.createdAt = LocalDateTime.now();
    }

    /** 단건 읽음 처리. */
    public void markAsRead() {
        if (this.readAt == null) {
            this.readAt = LocalDateTime.now();
        }
    }

    public boolean isRead() {
        return this.readAt != null;
    }
}
