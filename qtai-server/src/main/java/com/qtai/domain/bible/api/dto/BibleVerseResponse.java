package com.qtai.domain.bible.api.dto;

/** 성경 절 응답 DTO. */
public record BibleVerseResponse(
        // TODO: Long id
        // TODO: String book          — 책 이름 (예: "요한복음")
        // TODO: Integer chapter      — 장 번호
        // TODO: Integer verse        — 절 번호
        // TODO: String text          — 절 본문
        // TODO: String translation   — 번역본 코드
        // 표시용: "요한복음 3:16" 형태는 호출자가 조립
) {}
