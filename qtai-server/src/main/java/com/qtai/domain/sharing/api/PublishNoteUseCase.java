package com.qtai.domain.sharing.api;

import com.qtai.domain.sharing.api.dto.SharingPostResponse;
import com.qtai.domain.sharing.api.dto.PublishNoteRequest;

/**
 * 노트를 나눔(커뮤니티 피드)으로 공개하는 UseCase 포트.
 *
 * POST /api/v1/notes/{noteId}/share
 *
 * 정책:
 * - confirmNicknamePublic=true 필수 (미전달 시 400)
 * - 노트 본문을 SharingPost 에 스냅샷으로 복사 (원자성 보장)
 * - 공개 후 원본 노트 수정은 게시글에 반영되지 않는다
 * - commentsEnabled 는 작성자가 선택 (기본 true)
 */
public interface PublishNoteUseCase {

    SharingPostResponse publish(Long memberId, Long noteId, PublishNoteRequest request);
}
