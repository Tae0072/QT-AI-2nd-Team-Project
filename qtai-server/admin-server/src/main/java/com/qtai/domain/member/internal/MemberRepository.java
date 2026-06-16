package com.qtai.domain.member.internal;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    /**
     * 관리자 회원 목록 검색(F-04/F-10). status가 null이면 전체 상태,
     * q가 null이면 닉네임 필터를 건너뛴다. q는 닉네임 "포함" 검색.
     */
    @Query("""
            SELECT m FROM Member m
            WHERE (:status IS NULL OR m.status = :status)
              AND (:q IS NULL OR m.nickname LIKE CONCAT('%', :q, '%') ESCAPE '\\')
            """)
    Page<Member> searchForAdmin(@Param("status") MemberStatus status,
                                @Param("q") String q,
                                Pageable pageable);
}
