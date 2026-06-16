package com.qtai.domain.sharing.api;

import com.qtai.domain.sharing.api.dto.SharingPostListResponse;
import org.springframework.data.domain.Pageable;

/**
 * 내가 태그(멘션)된 글 목록 조회 UseCase 포트.
 *
 * GET /api/v1/me/mentions — 내가 '#닉네임'으로 멘션된 나눔 글을 최근 글 순으로 반환한다.
 * 응답은 피드 목록과 동일한 {@link SharingPostListResponse}를 재사용한다(같은 카드 렌더링).
 * 한 글에 여러 번 멘션돼도 글은 한 번만 나오고, 숨김·삭제된 글은 제외된다.
 */
public interface ListMentionsUseCase {

    SharingPostListResponse listMentions(Long memberId, Pageable pageable);
}
