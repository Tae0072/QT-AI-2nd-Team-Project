package com.qtai.domain.praise.api;

import com.qtai.domain.praise.api.dto.MemberPraiseSongResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * 내 찬양 목록 조회 UseCase 포트.
 */
public interface ListMemberPraiseSongUseCase {

    Page<MemberPraiseSongResponse> listMy(Long memberId, Pageable pageable);

    long countMy(Long memberId);
}
