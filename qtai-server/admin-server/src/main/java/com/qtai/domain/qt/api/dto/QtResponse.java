package com.qtai.domain.qt.api.dto;

/** QT 응답 DTO. */
public record QtResponse(
        // TODO: Long id
        // TODO: Long memberId               — 작성자
        // TODO: String authorNickname       — 작성자 닉네임 (조회 시 N+1 회피 위해 join 또는 별도 조회)
        // TODO: String title
        // TODO: String content
        // TODO: Long bibleVerseId           — null 가능
        // TODO: String bibleVerseRef        — "요한복음 3:16" 형태 표시용
        // TODO: LocalDateTime createdAt
        // TODO: LocalDateTime updatedAt
) {}
