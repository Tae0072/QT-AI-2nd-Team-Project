package com.qtai.domain.sharing.api.dto;

import java.time.LocalDateTime;

/**
 * 나눔 피드 1건 (04 §4.4.1 GET /api/v1/sharing-posts).
 *
 * 목록용이라 본문은 미리보기(bodyPreview)만 담는다. 전체 본문·절 배열은 상세(§4.4.2)에서 제공한다.
 *
 * @param nicknameSnapshot   발행 시점 작성자 닉네임 박제 (07 §F-10)
 * @param bodyPreview        본문 미리보기 (앞 일부만 잘라서 제공)
 * @param sourceNoteDeletedAt 원본 노트 공유 해제 시각. null이면 원본 유효
 * @param likedByMe          현재 조회자가 좋아요를 눌렀는지 (사용자별 계산)
 * @param publishedAt        발행 시각
 */
public record SharingPostListItem(
        Long id,
        String nicknameSnapshot,
        String titleSnapshot,
        String category,
        String status,
        VerseSnapshot verseSnapshot,
        String bodyPreview,
        boolean commentsEnabled,
        LocalDateTime sourceNoteDeletedAt,
        int likeCount,
        int commentCount,
        boolean likedByMe,
        LocalDateTime publishedAt
) {}
