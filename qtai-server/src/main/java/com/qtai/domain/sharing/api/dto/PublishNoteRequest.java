package com.qtai.domain.sharing.api.dto;

/**
 * 노트 나눔 공개 요청 DTO.
 *
 * POST /api/v1/notes/{noteId}/share
 *
 * confirmNicknamePublic=true 가 없으면 400 Bad Request.
 */
public record PublishNoteRequest(
    // TODO: @NotNull boolean confirmNicknamePublic;   — 닉네임 공개 동의 (true 필수)
    // TODO: Boolean commentsEnabled;                 — 댓글 허용 여부 (기본 true)
) {}
