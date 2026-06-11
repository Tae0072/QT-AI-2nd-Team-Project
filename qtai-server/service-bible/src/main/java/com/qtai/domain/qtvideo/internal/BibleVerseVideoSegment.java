package com.qtai.domain.qtvideo.internal;

import com.qtai.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(
        name = "bible_verse_video_segments",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_bv_video_segments_verse_source",
                columnNames = {"bible_verse_id", "source_video_id"}
        ),
        indexes = @Index(name = "idx_bv_video_segments_source_time", columnList = "source_video_id, start_time_sec")
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BibleVerseVideoSegment extends BaseEntity {

    @Column(name = "bible_verse_id", nullable = false)
    private Long bibleVerseId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "source_video_id", nullable = false)
    private SourceVideo sourceVideo;

    @Column(name = "start_time_sec", nullable = false, precision = 10, scale = 3)
    private BigDecimal startTimeSec;

    @Column(name = "end_time_sec", nullable = false, precision = 10, scale = 3)
    private BigDecimal endTimeSec;
}
