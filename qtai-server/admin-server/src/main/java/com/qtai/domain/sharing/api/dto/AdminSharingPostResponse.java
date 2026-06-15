package com.qtai.domain.sharing.api.dto;

import java.time.LocalDateTime;

/**
 * 관리자 나눔 글 목록·상세 응답 DTO (F-10, AD-15).
 *
 * <p>사용자용 응답과 달리 모든 상태(PUBLISHED/HIDDEN/DELETED)를 볼 수 있다.
 * 목록은 미리보기(bodyPreview)만, 상세(getForAdmin)는 전체 본문(body)·절 라벨·QT 날짜까지 담는다.
 * 작성자 식별은 닉네임 스냅샷으로만 한다.
 */
public record AdminSharingPostResponse(
        Long id,
        Long memberId,
        String nicknameSnapshot,
        String titleSnapshot,
        String category,
        String status,
        String bodyPreview,
        String body,
        String verseLabel,
        String qtDate,
        boolean commentsEnabled,
        int likeCount,
        int commentCount,
        LocalDateTime hiddenAt,
        LocalDateTime sourceNoteUnsharedAt,
        LocalDateTime createdAt
) {
}
