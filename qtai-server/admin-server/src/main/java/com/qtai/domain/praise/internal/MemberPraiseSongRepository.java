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

    /** 특정 회원 + 디바이스 곡 중복 확인. */
    boolean existsByMemberIdAndDeviceSongKey(Long memberId, String deviceSongKey);

    /** 특정 회원의 저장 곡 수. */
    long countByMemberId(Long memberId);

    /**
     * 큐레이션 곡을 참조하는 모든 회원 저장 행 삭제.
     *
     * <p>관리자가 큐레이션 곡을 삭제할 때 fk_mps_praise_song(RESTRICT) 위반을
     * 막기 위해 참조 행을 먼저 정리하는 데 사용한다. 회원 저장 행은 곡을 가리키는
     * 순수 참조 메타데이터(사용자 작성 콘텐츠 아님)이므로 곡 삭제 시 함께 정리한다.
     *
     * @return 삭제된 회원 저장 행 수
     */
    long deleteByPraiseSongId(Long praiseSongId);
}
