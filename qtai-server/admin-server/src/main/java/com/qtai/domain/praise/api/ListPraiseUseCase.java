package com.qtai.domain.praise.api;

import com.qtai.domain.praise.api.dto.PraiseResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * 찬양 큐레이션 목록 조회 UseCase 포트.
 */
public interface ListPraiseUseCase {

    /** 사용자용: ACTIVE 곡만 반환. */
    Page<PraiseResponse> listActive(Pageable pageable);

    /** 관리자용: 상태 필터 포함 전체 조회. status null 이면 전체. */
    Page<PraiseResponse> listAdmin(String status, Pageable pageable);
}
