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
}
