package com.qtai.domain.praise.api;

import com.qtai.domain.praise.api.dto.PraiseResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * 찬양 큐레이션 목록 조회 UseCase 포트.
 */
public interface ListPraiseUseCase {

    Page<PraiseResponse> listActive(Pageable pageable);
}
