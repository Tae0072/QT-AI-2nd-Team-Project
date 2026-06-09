package com.qtai.domain.sharing.api.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;

/**
 * 노트 나눔 공개 요청 DTO.
 *
 * <p>POST /api/v1/notes/{noteId}/share
 * <p>confirmNicknamePublic=true 가 없으면 400 Bad Request.
 *
 * @param confirmNicknamePublic 닉네임 공개 동의 (null 불가, true 필수)
 * @param commentsEnabled 댓글 허용 여부 (null이면 기본 true)
 */
public record PublishNoteRequest(
        @NotNull(message = "닉네임 공개 동의(confirmNicknamePublic) 값은 필수입니다")
        @AssertTrue(message = "닉네임 공개 동의(confirmNicknamePublic=true)가 필요합니다")
        Boolean confirmNicknamePublic,
        Boolean commentsEnabled
) {
    /** commentsEnabled null이면 기본 true. */
    public boolean isCommentsEnabled() {
        return commentsEnabled == null || commentsEnabled;
    }
}
