package com.qtai.domain.sharing.api;

import com.qtai.domain.sharing.api.dto.SharingPostResponse;

/**
 * 나눔 글 상세 조회 UseCase 포트.
 *
 * GET /api/v1/sharing-posts/{postId}
 *
 * 정책:
 * - PUBLISHED 상태만 조회 가능. HIDDEN/DELETED/없는 글은 404 NOT_FOUND (존재를 숨김)
 * - likedByMe(조회자 좋아요 여부), ownedByMe(조회자=작성자 여부)는 memberId 기준 계산
 */
public interface GetSharingPostUseCase {

    SharingPostResponse getDetail(Long memberId, Long postId);
}
