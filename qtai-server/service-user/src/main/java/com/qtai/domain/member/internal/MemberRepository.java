package com.qtai.domain.member.internal;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * 회원 영속성 포트. Spring Data JPA로 구현.
 */
public interface MemberRepository extends JpaRepository<Member, Long> {

    Optional<Member> findByKakaoId(Long kakaoId);

    boolean existsByKakaoId(Long kakaoId);

    boolean existsByNickname(String nickname);

    /** 닉네임 정확 일치 일괄 조회 — '#닉네임' 멘션 해석(닉네임→회원)용. */
    List<Member> findByNicknameIn(Collection<String> nicknames);

    /** 닉네임 접두사 검색(대소문자 무시) — 멘션 자동완성용. 정렬·개수 제한은 Pageable로. */
    List<Member> findByNicknameStartingWithIgnoreCaseOrderByNicknameAsc(String prefix, Pageable pageable);
}
