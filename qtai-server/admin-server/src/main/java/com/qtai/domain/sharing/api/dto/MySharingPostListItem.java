package com.qtai.domain.sharing.api.dto;

import java.time.LocalDateTime;

/**
 * 내 나눔 목록 1건 (04 §4.4.5 GET /api/v1/me/sharing-posts, 화면 M-05).
 *
 * 공개 피드 항목({@link SharingPostListItem})과 달리, 작성자 본인이 보는 관리용 목록이라
 * 닉네임 스냅샷·구절 스냅샷·본문 미리보기·likedByMe를 담지 않는다(04 계약 기준).
 *
 * @param status              글 상태(PUBLISHED/HIDDEN). 화면에서 되돌리기/숨김 버튼 분기에 사용
 * @param sourceNoteDeletedAt 원본 노트 공유 해제 시각. null이면 원본 유효
 * @param publishedAt         발행 시각
 */
public record MySharingPostListItem(
        Long id,
        String titleSnapshot,
        String category,
        String status,
        boolean commentsEnabled,
        LocalDateTime sourceNoteDeletedAt,
        int likeCount,
        int commentCount,
        LocalDateTime publishedAt
) {}
