package com.qtai.domain.sharing.api;

import com.qtai.domain.sharing.api.dto.MySharingPostListResponse;
import org.springframework.data.domain.Pageable;

/**
 * 내 나눔 목록 조회 UseCase 포트 (04 §4.4.5 GET /api/v1/me/sharing-posts, 화면 M-05).
 *
 * 공개 피드({@link ListSharingPostsUseCase})와 달리 "작성자 본인"의 글을 조회한다.
 *
 * 정책:
 * - 본인(memberId)이 작성한 글만 반환한다.
 * - status가 null이면 PUBLISHED + HIDDEN을 함께 반환한다(삭제본 DELETED는 항상 제외).
 *   숨긴 글까지 보여야 M-05에서 되돌리기(show)를 누를 수 있다.
 * - status로 PUBLISHED 또는 HIDDEN을 명시하면 해당 상태만 필터링한다.
 * - status가 DELETED이거나 정의되지 않은 값이면 400(INVALID_INPUT)으로 막는다.
 */
public interface ListMySharingPostsUseCase {

    MySharingPostListResponse listMine(Long memberId, String status, Pageable pageable);
}
