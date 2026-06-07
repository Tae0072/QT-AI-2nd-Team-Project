package com.qtai.domain.member.internal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 사용자 설정 엔티티.
 *
 * <p>member_id 1:1 매핑. 첫 조회 시 기본값으로 자동 생성.
 */
@Entity
@Table(name = "member_settings", uniqueConstraints = {
        @UniqueConstraint(columnNames = "member_id")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MemberSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "member_id", nullable = false, unique = true)
    private Long memberId;

    @Column(name = "notification_enabled", nullable = false)
    private Boolean notificationEnabled;

    @Enumerated(EnumType.STRING)
    @Column(name = "font_size", nullable = false, length = 10)
    private FontSize fontSize;

    /** 배경음악 켜기/끄기 (기본 ON). */
    @Column(name = "music_enabled", nullable = false)
    private Boolean musicEnabled;

    /** 배경음악 볼륨 0~100. */
    @Column(name = "music_volume", nullable = false)
    private Integer musicVolume;

    /** 배경음악 재생 대상: ALL | BGM | HYMN. */
    @Column(name = "music_category", nullable = false, length = 10)
    private String musicCategory;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /** 기본값으로 생성. */
    public static MemberSettings createDefault(Long memberId) {
        MemberSettings settings = new MemberSettings();
        settings.memberId = memberId;
        settings.notificationEnabled = true;
        settings.fontSize = FontSize.MEDIUM;
        settings.musicEnabled = true;
        settings.musicVolume = 70;
        settings.musicCategory = "BGM";
        settings.createdAt = LocalDateTime.now();
        settings.updatedAt = LocalDateTime.now();
        return settings;
    }

    /** 알림 수신 설정 변경. */
    public void updateNotificationEnabled(Boolean enabled) {
        if (enabled != null) {
            this.notificationEnabled = enabled;
            this.updatedAt = LocalDateTime.now();
        }
    }

    /** 폰트 크기 변경. */
    public void updateFontSize(FontSize fontSize) {
        if (fontSize != null) {
            this.fontSize = fontSize;
            this.updatedAt = LocalDateTime.now();
        }
    }

    /** 배경음악 켜기/끄기 변경. */
    public void updateMusicEnabled(Boolean enabled) {
        if (enabled != null) {
            this.musicEnabled = enabled;
            this.updatedAt = LocalDateTime.now();
        }
    }

    /** 배경음악 볼륨 변경(0~100 범위로 보정). */
    public void updateMusicVolume(Integer volume) {
        if (volume != null) {
            this.musicVolume = Math.max(0, Math.min(100, volume));
            this.updatedAt = LocalDateTime.now();
        }
    }

    /** 배경음악 재생 대상 변경(ALL/BGM/HYMN). */
    public void updateMusicCategory(String category) {
        if (category != null) {
            this.musicCategory = category;
            this.updatedAt = LocalDateTime.now();
        }
    }
}
