package com.qtai.domain.sharing.api;

/**
 * 신고 처리(모더레이션) 강제 숨김 포트 — report 도메인이 호출한다.
 *
 * <p>{@link SharingPostVisibilityUseCase}는 작성자 본인 검증이 붙은 사용자용
 * 계약이라, 관리자 신고 처리(HIDE_TARGET)용으로 소유자 검증 없는 별도 포트를 둔다
 * (명세 §4.7.4 — 신고 RESOLVED 시 대상 콘텐츠 숨김).
 */
public interface HideSharingPostForModerationUseCase {

    /**
     * 게시글을 강제 숨김(HIDDEN) 처리한다.
     * 이미 숨김·삭제됐거나 존재하지 않으면 아무것도 하지 않는다(멱등).
     *
     * @param postId 숨길 게시글 ID
     */
    void hideForModeration(Long postId);
}
