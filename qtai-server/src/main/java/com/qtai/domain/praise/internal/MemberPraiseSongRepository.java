package com.qtai.domain.praise.internal;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * 내 찬양 목록 영속성 포트.
 */
public interface MemberPraiseSongRepository extends JpaRepository<MemberPraiseSong, Long> {

    /** 특정 회원의 내 찬양 목록 (최신순). */
    Page<MemberPraiseSong> findByMemberIdOrderByCreatedAtDesc(Long memberId, Pageable pageable);

    /** 특정 회원 + 큐레이션 곡 중복 확인. */
    boolean existsByMemberIdAndPraiseSongId(Long memberId, Long praiseSongId);

    /** 특정 회원의 특정 저장 곡 조회 (삭제 시 본인 검증용). */
    Optional<MemberPraiseSong> findByIdAndMemberId(Long id, Long memberId);

    /** 특정 회원의 저장 곡 수. */
    long countByMemberId(Long memberId);
}
