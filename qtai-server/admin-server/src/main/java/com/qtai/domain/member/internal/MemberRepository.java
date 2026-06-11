package com.qtai.domain.member.internal;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

/**
 * 회원 영속성 포트. Spring Data JPA로 구현.
 */
public interface MemberRepository extends JpaRepository<Member, Long> {

    Optional<Member> findByKakaoId(Long kakaoId);

    boolean existsByKakaoId(Long kakaoId);

    boolean existsByNickname(String nickname);

    @Query("select m.id from Member m where m.status = com.qtai.domain.member.internal.MemberStatus.ACTIVE")
    List<Long> findActiveMemberIds();
}
