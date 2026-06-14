package com.qtai.domain.sharing.api.dto;

import java.time.LocalDateTime;

/**
 * 나눔 글 상세 응답 (04 §4.4.2 GET /api/v1/sharing-posts/{postId}).
 *
 * 목록(§4.4.1, {@link SharingPostListItem})과 달리 본문 전체(bodySnapshot)와 절 배열,
 * ownedByMe·hiddenAt·deletedAt 등 상세 전용 필드를 포함한다.
 *
 * @param bodySnapshot       발행 시점 본문 전체 (목록의 미리보기와 달리 자르지 않음)
 * @param ownedByMe          조회자가 작성자인지
 * @param likedByMe          조회자가 좋아요를 눌렀는지
 * @param bookmarkedByMe     조회자가 저장(북마크)했는지
 * @param sourceNoteDeletedAt 원본 노트 공유 해제 시각. null이면 원본 유효
 */
public record SharingPostResponse(
        Long id,
        Long noteId,
        Long memberId,
        String nicknameSnapshot,
        String titleSnapshot,
        String bodySnapshot,
        String category,
        VerseSnapshotDetail verseSnapshot,
        boolean commentsEnabled,
        LocalDateTime sourceNoteDeletedAt,
        String status,
        int likeCount,
        int commentCount,
        boolean likedByMe,
        boolean bookmarkedByMe,
        boolean ownedByMe,
        LocalDateTime publishedAt,
        LocalDateTime hiddenAt,
        LocalDateTime deletedAt
) {}
