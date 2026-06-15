package com.qtai.domain.appversion.internal;

import com.qtai.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 업데이트 예정 항목(V44).
 *
 * <p>앱 재설치/스토어 업데이트가 필요해 즉시 반영할 수 없는 변경을 모아둔다.
 * 관리자가 '적용'하면 앱 출시 버전이 {@code targetAppVersion}으로 올라가고 상태가 APPLIED로 바뀐다.
 */
@Entity
@Table(name = "pending_app_updates")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PendingAppUpdate extends BaseEntity {

    @Column(name = "title", nullable = false, length = 150)
    private String title;

    @Column(name = "description", length = 1000)
    private String description;

    @Column(name = "target_app_version", nullable = false, length = 40)
    private String targetAppVersion;

    @Enumerated(EnumType.STRING)
    @Column(name = "update_mode", nullable = false, length = 20)
    private AppUpdateMode updateMode;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private PendingUpdateStatus status;

    @Column(name = "applied_at")
    private LocalDateTime appliedAt;

    @Builder
    public PendingAppUpdate(String title, String description, String targetAppVersion,
                            AppUpdateMode updateMode) {
        this.title = title;
        this.description = description;
        this.targetAppVersion = targetAppVersion;
        this.updateMode = (updateMode != null) ? updateMode : AppUpdateMode.RECOMMENDED;
        this.status = PendingUpdateStatus.PENDING;
    }

    /** 적용 처리(앱 출시 버전 반영 후 호출). */
    public void markApplied(LocalDateTime now) {
        this.status = PendingUpdateStatus.APPLIED;
        this.appliedAt = now;
    }

    /** 소프트 삭제(목록에서 제거). */
    public void softDelete(LocalDateTime now) {
        markDeletedAt(now);
    }
}
