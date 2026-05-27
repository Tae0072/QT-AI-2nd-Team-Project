package com.qtai.domain.study.internal;

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

import java.time.LocalDateTime;

@Entity
@Table(
        name = "verse_explanations",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_explanations_active_per_verse",
                columnNames = {"bible_verse_id", "active_unique_key"}
        ),
        indexes = {
                @Index(name = "idx_explanations_verse_status", columnList = "bible_verse_id, status"),
                @Index(name = "idx_explanations_status", columnList = "status")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class VerseExplanation extends BaseEntity {

    @Column(name = "bible_verse_id", nullable = false)
    private Long bibleVerseId;

    @Column(length = 300)
    private String summary;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String explanation;

    @Column(name = "source_label", nullable = false, length = 200)
    private String sourceLabel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private VerseExplanationStatus status = VerseExplanationStatus.PENDING;

    @Column(name = "active_unique_key", length = 20)
    private String activeUniqueKey;

    @Column(name = "ai_asset_id")
    private Long aiAssetId;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;
}
