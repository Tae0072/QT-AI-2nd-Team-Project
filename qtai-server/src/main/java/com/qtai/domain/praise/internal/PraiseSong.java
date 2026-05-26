package com.qtai.domain.praise.internal;

import com.qtai.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 찬양 큐레이션 곡 엔티티.
 *
 * ERD: praise_songs 테이블 (V7).
 * 저작권 정책: 가사·음원 본문 저장 금지 — 메타정보만 보관.
 */
@Entity
@Table(name = "praise_songs")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PraiseSong extends BaseEntity {

    @Column(name = "title", nullable = false, length = 100)
    private String title;

    @Column(name = "artist", length = 100)
    private String artist;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 20)
    private PraiseSourceType sourceType;

    /** 저작권 확인 메모 (관리자용). */
    @Column(name = "license_note", length = 300)
    private String licenseNote;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private PraiseSongStatus status;

    @Builder
    public PraiseSong(String title, String artist, PraiseSourceType sourceType,
                      String licenseNote, PraiseSongStatus status) {
        this.title = title;
        this.artist = artist;
        this.sourceType = (sourceType != null) ? sourceType : PraiseSourceType.CURATED;
        this.licenseNote = licenseNote;
        this.status = (status != null) ? status : PraiseSongStatus.ACTIVE;
    }

    public void hide() {
        this.status = PraiseSongStatus.HIDDEN;
    }

    public boolean isActive() {
        return PraiseSongStatus.ACTIVE == this.status;
    }
}
