package com.qtai.domain.praise.internal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 내 찬양 목록 엔티티.
 *
 * <p>ERD: member_praise_songs 테이블 (V8).
 * <p>큐레이션 곡 또는 디바이스 로컬 회원 등록.
 * <p>praise_song_id 가 null 이면 디바이스 전용 곡.
 */
@Entity
@Table(name = "member_praise_songs",
        indexes = @Index(name = "idx_member_praise_member",
                columnList = "member_id, created_at"),
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_member_praise_curated",
                        columnNames = {"member_id", "praise_song_id"}),
                @UniqueConstraint(name = "uk_member_praise_device",
                        columnNames = {"member_id", "device_song_key"})})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MemberPraiseSong {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 회원 ID (members.id FK — Long FK, 도메인 경계 준수). */
    @Column(name = "member_id", nullable = false)
    private Long memberId;

    /** 큐레이션 곡 ID (nullable — 디바이스 전용이면 null). */
    @Column(name = "praise_song_id")
    private Long praiseSongId;

    /** 디바이스 회원 식별키 (nullable). */
    @Column(name = "device_song_key", length = 200)
    private String deviceSongKey;

    /** 목록 표시명 (필수). */
    @Column(name = "display_title", nullable = false, length = 100)
    private String displayTitle;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    public MemberPraiseSong(Long memberId, Long praiseSongId,
                            String deviceSongKey, String displayTitle,
                            LocalDateTime createdAt) {
        this.memberId = memberId;
        this.praiseSongId = praiseSongId;
        this.deviceSongKey = deviceSongKey;
        this.displayTitle = displayTitle;
        this.createdAt = createdAt;
    }
}
