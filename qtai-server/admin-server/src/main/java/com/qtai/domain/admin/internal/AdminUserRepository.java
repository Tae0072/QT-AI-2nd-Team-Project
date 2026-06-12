package com.qtai.domain.admin.internal;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * 관리자 계정 Repository.
 *
 * <p>member_id로 조회하는 것이 주 패턴이다.
 * Spring Security가 JWT에서 memberId를 추출하면, 이 Repository로 admin_users를 조회한다.
 */
public interface AdminUserRepository extends JpaRepository<AdminUser, Long> {

    /** 회원 ID로 관리자 계정 조회. */
    Optional<AdminUser> findByMemberId(Long memberId);

    /** 로그인 아이디로 관리자 계정 조회(자체 아이디/비밀번호 로그인). */
    Optional<AdminUser> findByUsername(String username);

    /** 회원 ID로 활성 관리자 계정 존재 여부 확인. */
    boolean existsByMemberIdAndStatus(Long memberId, AdminStatus status);
}
