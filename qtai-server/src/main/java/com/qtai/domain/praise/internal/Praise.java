package com.qtai.domain.praise.internal;

/**
 * 찬양 엔티티 (메타정보 only).
 *
 * 저작권 정책: lyricsText, audioUrl 같이 본문/음원을 자체 보관하는 필드 추가 금지.
 * 외부 플랫폼 링크(externalLink)만 보관해 라이선스 책임을 분리한다.
 */
// TODO: @Entity, @Table(name = "praise")
public class Praise {

    // TODO: @Id @GeneratedValue Long id;
    // TODO: String title;
    // TODO: String artist;
    // TODO: String externalLink;     — 유튜브/멜론 등 외부 링크
    // TODO: String category;
    // TODO: LocalDateTime createdAt; — @CreationTimestamp
    // ※ lyrics_text / audio_url 컬럼 추가 금지 (v3.1)
}
