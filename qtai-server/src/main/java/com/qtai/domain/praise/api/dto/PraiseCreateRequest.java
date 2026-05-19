package com.qtai.domain.praise.api.dto;

/**
 * 찬양 등록 요청 DTO.
 *
 * v3.1 게이트: lyricsText, audioUrl 등 본문/음원 직접 저장 필드 추가 금지 —
 * 저작권 침해 리스크 회피.
 */
public record PraiseCreateRequest(
        // TODO: String title         — 곡명 (필수)
        // TODO: String artist        — 아티스트/팀
        // TODO: String externalLink  — 유튜브/멜론 등 외부 링크 (가사/음원 직접 보유 X)
        // TODO: String category      — 찬양 / CCM / 묵상송 등 분류
) {}
