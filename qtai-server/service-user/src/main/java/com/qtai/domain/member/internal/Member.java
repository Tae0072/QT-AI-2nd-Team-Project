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
 * <p>탈퇴 정책(2026-06-05 Lead 결정): 즉시 익명화하지 않고 개인정보를 2년 보존한다(탈퇴 시 고지).
 * 보존기간 만료 삭제는 별도 배치(후속 작업), 보존 중 재로그인 시 기존 계정을 재활성화한다.
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
     * 닉네임 변경 가능 여부 — 항상 변경 가능.
     *
     * <p>정책 변경(2026-06-11 마이페이지 피드백): 기존 7일 잠금(2026-05-19 결정)을 폐지하고
     * 즉시 변경을 허용한다. 잠금 부활 가능성에 대비해 판별 메서드와
     * {@code nicknameChangedAt} 기록(온보딩 완료 판별에도 사용 — AuthService 참조)은 유지한다.
     *
     * @param clock 시그니처 유지용(잠금 부활 대비) — 현재 미사용
     */
    public boolean isNicknameChangeable(Clock clock) {
        return true;
    }

    /** 닉네임 잠금 해제 시각 — 잠금 폐지로 항상 null(즉시 변경 가능). */
    public LocalDateTime getNicknameUnlockAt() {
        return null;
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
     * 회원 탈퇴 — 상태 전환 + 탈퇴 시각 기록.
     *
     * <p>정책: 개인정보(닉네임·이메일·kakaoId 등)는 즉시 익명화하지 않고 2년 보존한다.
     * 익명화하면 member_auth_providers의 (provider, provider_user_id) UNIQUE와 충돌해
     * 재가입이 영구 차단되는 문제가 있었다(M0009). 보존 중 재로그인은
     * {@link #reactivate(String, String)}로 기존 계정을 복구한다.
     * 보존기간 만료 삭제는 별도 배치(후속 작업)가 수행한다.
     *
     * @param clock 테스트 주입용 시계
     */
    public void withdraw(Clock clock) {
        this.status = MemberStatus.WITHDRAWN;
        this.withdrawnAt = LocalDateTime.now(clock);
    }

    /**
     * 탈퇴 회원 재활성화 — 보존기간(2년) 내 같은 카카오 계정으로 재로그인 시 기존 계정 복구.
     *
     * <p>이메일·프로필 이미지는 카카오 최신 값이 <b>있을 때만</b> 갱신한다. 카카오가 선택 동의 항목
     * (이메일·프로필)을 주지 않아 null로 오면 기존 보관 값을 유지한다(과거 값 소실 방지).
     * 닉네임과 nicknameChangedAt(온보딩 완료 판별용)은 기존 값을 유지한다.
     */
    public void reactivate(String email, String profileImageUrl) {
        this.status = MemberStatus.ACTIVE;
        this.withdrawnAt = null;
        if (email != null) {
            this.email = email;
        }
        if (profileImageUrl != null) {
            this.profileImageUrl = profileImageUrl;
        }
    }

    public boolean isActive() {
        return this.status == MemberStatus.ACTIVE;
    }
}
