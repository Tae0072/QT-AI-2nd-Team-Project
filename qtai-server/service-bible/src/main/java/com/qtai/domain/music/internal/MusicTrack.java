package com.qtai.domain.music.internal;

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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * 배경음악 음원 엔티티 (music_tracks, V26).
 *
 * <p>정책: 07/25/CLAUDE.md §8의 '음원 DB 저장 금지'를 Lead 승인으로 music 도메인에 한정 예외 허용
 * (2026-06-07, 로열티프리/직접제작 음원 한정). 출처는 {@code licenseNote} 에 기록한다.
 *
 * <p>{@code audioData} 는 LONGBLOB. 목록 조회는 {@link MusicTrackSummary} projection 으로
 * 메타데이터만 읽고, 스트리밍 시에만 {@link MusicTrackAudioView} 로 바이트를 읽어
 * 목록 쿼리가 무거워지지 않게 한다.
 */
@Entity
@Table(name = "music_tracks")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MusicTrack extends BaseEntity {

    @Column(name = "title", nullable = false, length = 150)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 20)
    private MusicCategory category;

    @Column(name = "mime_type", nullable = false, length = 60)
    private String mimeType;

    @Column(name = "byte_size", nullable = false)
    private Long byteSize;

    @Column(name = "duration_sec")
    private Integer durationSec;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;

    @Column(name = "enabled", nullable = false)
    private Boolean enabled;

    /** 출처/라이선스 메모(로열티프리·직접제작 확인용). */
    @Column(name = "license_note", length = 300)
    private String licenseNote;

    // MySQL LONGBLOB 매핑. @Lob(byte[])는 기본 length=255라 Hibernate가 tinyblob을 기대해
    // longblob 컬럼과 schema-validation이 충돌한다. LONGVARBINARY로 명시해 일치시킨다.
    // (DDL은 각 방언이 알아서 렌더 — MySQL=longblob, H2=large binary. columnDefinition은
    //  H2 create-drop 테스트에서 깨질 수 있어 쓰지 않는다.)
    @JdbcTypeCode(SqlTypes.LONGVARBINARY)
    @Column(name = "audio_data", nullable = false)
    private byte[] audioData;

    @Builder
    public MusicTrack(String title, MusicCategory category, String mimeType, Long byteSize,
                      Integer durationSec, Integer sortOrder, Boolean enabled,
                      String licenseNote, byte[] audioData) {
        this.title = title;
        this.category = (category != null) ? category : MusicCategory.BGM;
        this.mimeType = (mimeType != null) ? mimeType : "audio/mpeg";
        this.byteSize = (byteSize != null) ? byteSize
                : (audioData != null ? (long) audioData.length : 0L);
        this.durationSec = durationSec;
        this.sortOrder = (sortOrder != null) ? sortOrder : 0;
        this.enabled = (enabled != null) ? enabled : Boolean.TRUE;
        this.licenseNote = licenseNote;
        this.audioData = audioData;
    }

    /** 노출 비활성화(soft disable). */
    public void disable() {
        this.enabled = false;
    }
}
