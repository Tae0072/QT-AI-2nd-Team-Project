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

import java.time.LocalDateTime;

/**
 * 회원 엔티티.
 *
 * 식별 모델: 우리 서비스의 내부 id(PK) + 외부 카카오 식별자(kakaoId).
 * 카카오 ID는 unique — 같은 카카오 계정으로 중복 가입 방지.
 */
@Entity
@Table(name = "members", uniqueConstraints = {
        @UniqueConstraint(name = "uk_members_kakao_id", columnNames = "kakao_id"),
        @UniqueConstraint(name = "uk_members_nickname", columnNames = "nickname")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Member extends BaseEntity {

    @Column(name = "kakao_id", nullable = false, unique = true)
    private Long kakaoId;

    @Column(name = "email", length = 100)
    private String email;

    @Column(name = "nickname", nullable = false, unique = true, length = 20)
    private String nickname;

    @Column(name = "profile_image_url", length = 500)
    private String profileImageUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private MemberStatus status;

    @Column(name = "role", nullable = false, length = 10)
    private String role;

    @Column(name = "nickname_changed_at")
    private LocalDateTime nicknameChangedAt;

    @Column(name = "withdrawn_at")
    private LocalDateTime withdrawnAt;

    @Builder
    public Member(Long kakaoId, String email, String nickname,
                  String profileImageUrl, MemberStatus status, String role) {
        this.kakaoId = kakaoId;
        this.email = email;
        this.nickname = nickname;
        this.profileImageUrl = profileImageUrl;
        this.status = (status != null) ? status : MemberStatus.ACTIVE;
        this.role = (role != null) ? role : "USER";
    }

    // ── 닉네임 변경 (7일 잠금) ──

    /**
     * 닉네임 변경 가능 여부.
     * nicknameChangedAt이 null이거나 7일 경과 시 변경 가능.
     */
    public boolean isNicknameChangeable() {
        if (nicknameChangedAt == null) {
            return true;
        }
        return nicknameChangedAt.plusDays(7).isBefore(LocalDateTime.now());
    }

    /**
     * 닉네임 잠금 해제 시각 — 클라이언트 안내용.
     */
    public LocalDateTime getNicknameUnlockAt() {
        if (nicknameChangedAt == null) {
            return null;
        }
        return nicknameChangedAt.plusDays(7);
    }

    /**
     * 닉네임 변경. 7일 잠금 검증은 호출자(Service)가 수행.
     */
    public void changeNickname(String newNickname) {
        this.nickname = newNickname;
        this.nicknameChangedAt = LocalDateTime.now();
    }

    // ── 프로필 수정 ──

    public void updateProfileImageUrl(String profileImageUrl) {
        this.profileImageUrl = profileImageUrl;
    }

    // ── 탈퇴 ──

    /**
     * 회원 탈퇴 처리 — status=WITHDRAWN + 익명화.
     */
    public void withdraw() {
        this.status = MemberStatus.WITHDRAWN;
        this.withdrawnAt = LocalDateTime.now();
        this.email = null;
        this.profileImageUrl = null;
        // kakaoId는 재가입 방지를 위해 유지하거나 정책에 따라 처리
    }

    public boolean isActive() {
        return this.status == MemberStatus.ACTIVE;
    }
}
