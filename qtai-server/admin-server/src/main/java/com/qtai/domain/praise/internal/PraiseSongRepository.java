package com.qtai.domain.praise.internal;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 찬양 큐레이션 곡 영속성 포트.
 */
public interface PraiseSongRepository extends JpaRepository<PraiseSong, Long> {

    /** 상태별 목록 조회. */
    Page<PraiseSong> findByStatus(PraiseSongStatus status, Pageable pageable);
}
