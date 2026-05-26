package com.qtai.domain.praise.api;

import com.qtai.domain.praise.api.dto.PraiseCreateRequest;
import com.qtai.domain.praise.api.dto.PraiseResponse;

/**
 * 찬양 큐레이션 곡 등록 UseCase 포트 (ADMIN only).
 *
 * 저작권 리스크 회피: 가사·음원 저장 금지, 메타정보만 보관.
 */
public interface CreatePraiseUseCase {

    PraiseResponse create(Long adminId, PraiseCreateRequest request);
}
