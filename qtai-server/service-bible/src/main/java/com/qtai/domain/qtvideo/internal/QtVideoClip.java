package com.qtai.domain.qtvideo.internal;

import com.qtai.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
import java.time.LocalDateTime;

@Entity
@Table(
        name = "qt_video_clips",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_qt_video_clips_passage_active",
                columnNames = {"qt_passage_id", "active_unique_key"}
        ),
        indexes = {
                @Index(name = "idx_qt_video_clips_passage_status", columnList = "qt_passage_id, status"),
                @Index(name = "idx_qt_video_clips_status", columnList = "status")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class QtVideoClip extends BaseEntity {

    public static final String ACTIVE_UNIQUE_KEY = "ACTIVE";

    @Column(name = "qt_passage_id", nullable = false)
    private Long qtPassageId;

    @Column(nullable = false, length = 200)
    private String title;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "source_video_id", nullable = false)
    private SourceVideo sourceVideo;

    @Enumerated(EnumType.STRING)
    @Column(name = "composition_type", nullable = false, length = 30)
    private QtVideoCompositionType compositionType = QtVideoCompositionType.SINGLE_CUT;

    @Column(name = "video_url", nullable = false, length = 2048)
    private String videoUrl;

    @Column(name = "start_time_sec", nullable = false, precision = 10, scale = 3)
    private BigDecimal startTimeSec;

    @Column(name = "end_time_sec", nullable = false, precision = 10, scale = 3)
    private BigDecimal endTimeSec;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private QtVideoClipStatus status = QtVideoClipStatus.APPROVED;

    @Column(name = "active_unique_key", length = 20)
    private String activeUniqueKey;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    public static QtVideoClip approvedSingleCut(
            Long qtPassageId,
            String title,
            SourceVideo sourceVideo,
            String videoUrl,
            BigDecimal startTimeSec,
            BigDecimal endTimeSec,
            LocalDateTime approvedAt) {
        QtVideoClip clip = new QtVideoClip();
        clip.qtPassageId = qtPassageId;
        clip.title = title;
        clip.sourceVideo = sourceVideo;
        clip.compositionType = QtVideoCompositionType.SINGLE_CUT;
        clip.videoUrl = videoUrl;
        clip.startTimeSec = startTimeSec;
        clip.endTimeSec = endTimeSec;
        clip.status = QtVideoClipStatus.APPROVED;
        clip.activeUniqueKey = ACTIVE_UNIQUE_KEY;
        clip.approvedAt = approvedAt;
        return clip;
    }

    public void hide() {
        this.status = QtVideoClipStatus.HIDDEN;
        this.activeUniqueKey = null;
    }
}
