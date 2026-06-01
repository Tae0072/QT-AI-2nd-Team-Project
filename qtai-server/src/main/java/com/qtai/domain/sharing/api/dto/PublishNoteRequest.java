package com.qtai.domain.sharing.api.dto;

import jakarta.validation.constraints.NotNull;

/**
 * 노트 나눔 공개 요청 DTO.
 *
 * POST /api/v1/notes/{noteId}/share
 *
 * - confirmNicknamePublic: 닉네임 공개 동의. 값이 없으면 400(@NotNull),
 *   값이 false면 비즈니스 검증에서 422(동의 안 함)로 막는다.
 * - commentsEnabled: 댓글 허용 여부. 생략(null) 시 기본 true.
 */
public record PublishNoteRequest(
        @NotNull Boolean confirmNicknamePublic,
        Boolean commentsEnabled
) {}
