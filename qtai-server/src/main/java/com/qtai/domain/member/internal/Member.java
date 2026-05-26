package com.qtai.domain.member.internal;

import com.qtai.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Clock;
import java.time.LocalDateTime;

/**
 * 회원 엔티티.
 *
 * <p>ERD: members 테이블.
 * <p>탈퇴 정책: 닉네임·kakaoId 포함 개인정보 전체 익명화.
 */
@Entity
@Table(name = "members", uniqueConstraints = {
        @UniqueConstraint(columnNames = "kakao_id"),
        @UniqueConstraint(columnNames = "nickname")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Member extends BaseEntity {

    @Column(name = "kakao_id", nullable = false, unique = true)
    private Long kakaoId;

    @Column(length = 100)
    private String email;

    @Column(nullable = false, length = 20, unique = true)
    private String nickname;

    @Column(name = "profile_image_url", length = 500)
    private String profileImageUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MemberStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private MemberRole role;

    @Column(name = "nickname_changed_at")
    private LocalDateTime nicknameChangedAt;

    @Column(name = "withdrawn_at")
    private LocalDateTime withdrawnAt;

    public enum MemberRole {
        USER, ADMIN
    }

    @Builder
    public Member(Long kakaoId, String email, String nickname, String profileImageUrl) {
        this.kakaoId = kakaoId;
        this.email = email;
        this.nickname = nickname;
        this.profileImageUrl = profileImageUrl;
        this.status = MemberStatus.ACTIVE;
        this.role = MemberRole.USER;
    }

    /**
     * 닉네임 변경 가능 여부.
     *
     * @param clock 테스트 주입용 시계
     */
    public boolean isNicknameChangeable(Clock clock) {
        if (nicknameChangedAt == null) {
            return true;
        }
        return nicknameChangedAt.plusDays(7).isBefore(LocalDateTime.now(clock));
    }

    /** 닉네임 잠금 해제 시각 (변경 이력 없으면 null). */
    public LocalDateTime getNicknameUnlockAt() {
        if (nicknameChangedAt == null) {
            return null;
        }
        return nicknameChangedAt.plusDays(7);
    }

    /**
     * 닉네임 변경.
     *
     * @param clock 테스트 주입용 시계
     */
    public void changeNickname(String newNickname, Clock clock) {
        this.nickname = newNickname;
        this.nicknameChangedAt = LocalDateTime.now(clock);
    }

    public void updateProfileImageUrl(String profileImageUrl) {
        this.profileImageUrl = profileImageUrl;
    }

    /**
     * 회원 탈퇴 — 개인정보 전체 익명화.
     *
     * <p>정책: 닉네임("탈퇴회원_{id}"), 이메일, 프로필 이미지, kakaoId 모두 제거.
     * <p>kakaoId: 음수 고유값(-id) 사용 — 0L 사용 시 두 번째 탈퇴 회원부터 kakao_id UNIQUE 위반.
     *
     * @param clock 테스트 주입용 시계
     */
    public void withdraw(Clock clock) {
        this.status = MemberStatus.WITHDRAWN;
        this.withdrawnAt = LocalDateTime.now(clock);
        this.nickname = "탈퇴회원_" + getId();
        this.email = null;
        this.profileImageUrl = null;
        this.kakaoId = -getId();
    }

    public boolean isActive() {
        return this.status == MemberStatus.ACTIVE;
    }
}
