package com.qtai.domain.praise.api;

import com.qtai.domain.praise.api.dto.MemberPraiseSongCreateRequest;
import com.qtai.domain.praise.api.dto.MemberPraiseSongResponse;

/**
 * 내 찬양 저장/삭제 UseCase 포트.
 */
public interface SaveMemberPraiseSongUseCase {

    MemberPraiseSongResponse save(Long memberId, MemberPraiseSongCreateRequest request);

    void remove(Long memberId, Long memberPraiseSongId);
}
