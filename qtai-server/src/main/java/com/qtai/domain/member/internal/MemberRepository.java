package com.qtai.domain.member.internal;

/**
 * 회원 영속성 포트. Spring Data JPA로 구현.
 */
public interface MemberRepository {

    // TODO: extends JpaRepository<Member, Long>
    // TODO: Optional<Member> findByKakaoId(Long kakaoId);   — 로그인 시 사용
    // TODO: boolean existsByNickname(String nickname);      — 닉네임 중복 체크
}
