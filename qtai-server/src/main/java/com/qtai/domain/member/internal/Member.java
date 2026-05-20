package com.qtai.domain.member.internal;

/**
 * 회원 엔티티.
 *
 * 식별 모델: 우리 서비스의 내부 id(PK) + 외부 카카오 식별자(kakaoId).
 * 카카오 ID는 unique — 같은 카카오 계정으로 중복 가입 방지.
 */
// TODO: @Entity, @Table(name = "member", uniqueConstraints = @UniqueConstraint(columnNames = "kakao_id"))
public class Member {

    // TODO: @Id @GeneratedValue Long id;
    // TODO: Long kakaoId;                  — 카카오 OAuth 식별자 (unique, not null)
    // TODO: String email;                  — 이메일 (선택 동의 — nullable)
    // TODO: String nickname;               — 닉네임 (unique, not null, 2~20자)
    // TODO: String profileImageUrl;
    // TODO: @Enumerated(STRING) MemberStatus status; — 기본 ACTIVE
    // TODO: @Enumerated(STRING) Role role;           — USER / ADMIN (기본 USER)
    // TODO: LocalDateTime joinedAt;        — @CreationTimestamp
    // TODO: LocalDateTime withdrawnAt;     — 탈퇴 시각 (nullable)
}
