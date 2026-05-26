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

import java.time.Clock;
import java.time.LocalDateTime;

/**
 * 알림 엔티티.
 *
 * <p>ERD: notifications 테이블 (V6).
 * <p>수신대상 (member_id, read_at, created_at) 복합 인덱스로 알림 필터 + 최신순 조회가 가능하다.
 * <p>event_key UNIQUE 로 동일 이벤트 중복 알림 방지.
 *
 * <p>설계 결정 — BaseEntity 미상속:
 * createdAt 은 서비스 레이어에서 Clock 주입으로 설정 (테스트 시간 제어 목적),
 * updatedAt 은 알림 특성상 불필요 (readAt 만 갱신). BaseEntity 의 @CreatedDate/@LastModifiedDate
 * 자동 설정과 Clock 주입 방식이 충돌하므로 의도적으로 분리.
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

    /** 공지 알림의 경우 notices.id (nullable). */
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

    /** 읽음 시각 (null 이면 미읽음). */
    @Column(name = "read_at")
    private LocalDateTime readAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    public Notification(Long memberId, String type, String title, String body,
                        Long noticeId, String linkType, Long linkId, String eventKey,
                        LocalDateTime createdAt) {
        this.memberId = memberId;
        this.type = type;
        this.title = title;
        this.body = body;
        this.noticeId = noticeId;
        this.linkType = linkType;
        this.linkId = linkId;
        this.eventKey = eventKey;
        this.createdAt = createdAt;
    }

    /** 개별 읽음 처리. */
    public void markAsRead(Clock clock) {
        if (this.readAt == null) {
            this.readAt = LocalDateTime.now(clock);
        }
    }

    public boolean isRead() {
        return this.readAt != null;
    }
}
