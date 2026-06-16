package com.qtai.domain.praise.api;

import com.qtai.domain.praise.api.dto.PraiseResponse;
import com.qtai.domain.praise.api.dto.PraiseUpdateRequest;

/**
 * 찬양 큐레이션 곡 수정 UseCase 포트 (ADMIN only).
 */
public interface UpdatePraiseUseCase {

    PraiseResponse update(Long adminId, Long praiseSongId, PraiseUpdateRequest request);
}
