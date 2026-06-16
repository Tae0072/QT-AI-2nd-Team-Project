package com.qtai.domain.appversion.internal;

import com.qtai.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 앱 버전 상태(단일 행, V44).
 *
 * <p>두 가지 버전을 함께 둔다.
 * <ul>
 *   <li>{@code contentVersion} — 콘텐츠/패치 버전(예: 0.1.0.1). 백그라운드 데이터 갱신으로
 *       즉시 반영되는 변경에 사용. 관리자 '적용(게시)'마다 마지막 자리를 올린다.</li>
 *   <li>{@code appVersion} — 앱 출시 버전(예: 0.1.0). 앱 재설치/스토어 업데이트가 필요한 변경에 사용.
 *       '업데이트 예정' 항목을 적용할 때 올라간다.</li>
 * </ul>
 */
@Entity
@Table(name = "app_version_state")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AppVersionState extends BaseEntity {

    @Column(name = "content_version", nullable = false, length = 40)
    private String contentVersion;

    @Column(name = "app_version", nullable = false, length = 40)
    private String appVersion;

    @Column(name = "min_supported_version", nullable = false, length = 40)
    private String minSupportedVersion;

    @Enumerated(EnumType.STRING)
    @Column(name = "update_mode", nullable = false, length = 20)
    private AppUpdateMode updateMode;

    @Column(name = "update_message", length = 300)
    private String updateMessage;

    @Builder
    public AppVersionState(String contentVersion, String appVersion, String minSupportedVersion,
                           AppUpdateMode updateMode, String updateMessage) {
        this.contentVersion = (contentVersion != null) ? contentVersion : "0.1.0";
        this.appVersion = (appVersion != null) ? appVersion : "0.1.0";
        this.minSupportedVersion = (minSupportedVersion != null) ? minSupportedVersion : "0.1.0";
        this.updateMode = (updateMode != null) ? updateMode : AppUpdateMode.NONE;
        this.updateMessage = updateMessage;
    }

    /** 콘텐츠 버전을 한 단계 올린다(즉시 게시). 예: 0.1.0 → 0.1.0.1, 0.1.0.1 → 0.1.0.2. */
    public void bumpContentVersion() {
        this.contentVersion = nextContentVersion(this.contentVersion);
    }

    /**
     * 앱 출시 버전을 올린다(업데이트 예정 적용). 콘텐츠 버전도 새 출시 버전을 기준으로 초기화한다.
     *
     * @param targetAppVersion 새 출시 버전(예: 0.2.0)
     * @param mode             적용 후 사용자 안내 강도
     * @param message          업데이트 안내 문구
     */
    public void promoteAppVersion(String targetAppVersion, AppUpdateMode mode, String message) {
        this.appVersion = targetAppVersion;
        this.contentVersion = targetAppVersion;
        this.updateMode = (mode != null) ? mode : AppUpdateMode.NONE;
        this.updateMessage = message;
        if (mode == AppUpdateMode.FORCED) {
            // 강제 업데이트: 새 출시 버전 미만은 차단.
            this.minSupportedVersion = targetAppVersion;
        }
    }

    /** 안내 강도/문구만 갱신(예: 강제→권장 완화). */
    public void changeUpdateNotice(AppUpdateMode mode, String message) {
        this.updateMode = (mode != null) ? mode : AppUpdateMode.NONE;
        this.updateMessage = message;
    }

    static String nextContentVersion(String current) {
        if (current == null || current.isBlank()) {
            return "0.1.0.1";
        }
        String[] parts = current.trim().split("\\.");
        if (parts.length >= 4) {
            try {
                long last = Long.parseLong(parts[parts.length - 1]) + 1;
                parts[parts.length - 1] = Long.toString(last);
                return String.join(".", parts);
            } catch (NumberFormatException e) {
                return current + ".1";
            }
        }
        // 3자리 이하면 패치 자리를 추가한다(0.1.0 → 0.1.0.1).
        return current + ".1";
    }
}
