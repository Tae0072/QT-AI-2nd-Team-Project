package com.qtai.domain.member.internal;

import com.qtai.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * OAuth 제공자 연동 정보.
 * 현재는 KAKAO만 지원하며, 향후 다른 소셜 로그인 추가 시 확장 가능.
 */
@Entity
@Table(name = "member_auth_providers", uniqueConstraints = {
        @UniqueConstraint(name = "uk_auth_provider_user", columnNames = {"provider", "provider_user_id"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MemberAuthProvider extends BaseEntity {

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(nullable = false, length = 20)
    private String provider;

    @Column(name = "provider_user_id", nullable = false, length = 100)
    private String providerUserId;

    @Column(name = "connected_at", nullable = false)
    private LocalDateTime connectedAt;

    @Builder
    public MemberAuthProvider(Long memberId, String provider, String providerUserId) {
        this.memberId = memberId;
        this.provider = provider;
        this.providerUserId = providerUserId;
        this.connectedAt = LocalDateTime.now();
    }
}
