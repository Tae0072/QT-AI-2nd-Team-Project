package com.qtai.domain.notification.internal;

import com.qtai.common.exception.BusinessException;
import com.qtai.common.exception.ErrorCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Clock;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "notices")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "admin_user_id", nullable = false)
    private Long adminUserId;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(nullable = false, columnDefinition = "LONGTEXT")
    private String body;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NoticeStatus status;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    private Notice(Long adminUserId, String title, String body) {
        this.adminUserId = adminUserId;
        this.title = requireText(title, "title", 100);
        this.body = requireText(body, "body", 10_000);
        this.status = NoticeStatus.DRAFT;
    }

    public static Notice draft(Long adminUserId, String title, String body) {
        if (adminUserId == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "관리자 ID가 필요합니다.");
        }
        return new Notice(adminUserId, title, body);
    }

    public void updateDraft(String title, String body) {
        if (this.status != NoticeStatus.DRAFT) {
            throw new BusinessException(ErrorCode.INVALID_STATUS_TRANSITION);
        }
        this.title = requireText(title, "title", 100);
        this.body = requireText(body, "body", 10_000);
    }

    public void publish(Clock clock) {
        if (this.status != NoticeStatus.DRAFT) {
            throw new BusinessException(ErrorCode.INVALID_STATUS_TRANSITION);
        }
        this.status = NoticeStatus.PUBLISHED;
        this.publishedAt = LocalDateTime.now(clock);
    }

    public void hide() {
        if (this.status == NoticeStatus.HIDDEN) {
            throw new BusinessException(ErrorCode.INVALID_STATUS_TRANSITION);
        }
        this.status = NoticeStatus.HIDDEN;
    }

    @PrePersist
    void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    private static String requireText(String value, String fieldName, int maxLength) {
        String normalized = value == null ? null : value.trim();
        if (normalized == null || normalized.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, fieldName + "은(는) 필수입니다.");
        }
        if (normalized.length() > maxLength) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, fieldName + " 길이가 너무 깁니다.");
        }
        return normalized;
    }
}
