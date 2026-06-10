package com.qtai.domain.qtvideo.internal;

import com.qtai.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(
        name = "source_videos",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_source_videos_book_active",
                columnNames = {"bible_book_id", "active_unique_key"}
        ),
        indexes = @Index(name = "idx_source_videos_book_status", columnList = "bible_book_id, status")
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SourceVideo extends BaseEntity {

    public static final String ACTIVE_UNIQUE_KEY = "ACTIVE";

    @Column(name = "bible_book_id", nullable = false)
    private Short bibleBookId;

    @Column(nullable = false, length = 200)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(name = "storage_provider", nullable = false, length = 30)
    private SourceVideoStorageProvider storageProvider = SourceVideoStorageProvider.EXTERNAL_URL;

    @Column(name = "video_url", nullable = false, length = 2048)
    private String videoUrl;

    @Column(name = "duration_sec", precision = 10, scale = 3)
    private BigDecimal durationSec;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SourceVideoStatus status = SourceVideoStatus.ACTIVE;

    @Column(name = "active_unique_key", length = 20)
    private String activeUniqueKey;

    public static SourceVideo active(
            Short bibleBookId,
            String title,
            SourceVideoStorageProvider storageProvider,
            String videoUrl,
            BigDecimal durationSec) {
        SourceVideo sourceVideo = new SourceVideo();
        sourceVideo.bibleBookId = bibleBookId;
        sourceVideo.title = title;
        sourceVideo.storageProvider = storageProvider;
        sourceVideo.videoUrl = videoUrl;
        sourceVideo.durationSec = durationSec;
        sourceVideo.status = SourceVideoStatus.ACTIVE;
        sourceVideo.activeUniqueKey = ACTIVE_UNIQUE_KEY;
        return sourceVideo;
    }
}
