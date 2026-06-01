package com.qtai.domain.sharing.api.dto;

/**
 * 좋아요 토글 응답 (04 §4.4.3).
 *
 * <p>좋아요 직후의 최신 상태를 돌려줘 클라이언트가 재조회 없이 UI를 갱신할 수 있게 한다.
 *
 * @param likeCount  COUNT 재계산으로 맞춘 현재 좋아요 수
 * @param likedByMe  요청자의 좋아요 여부 (like 직후 true)
 */
public record LikeResponse(
        long likeCount,
        boolean likedByMe
) {}
