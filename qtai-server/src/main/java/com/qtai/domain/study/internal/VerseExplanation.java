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
import java.util.Objects;

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

    private static final String ACTIVE_UNIQUE_KEY = "ACTIVE";

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

    public static VerseExplanation approvedFromAiAsset(
            Long bibleVerseId,
            String summary,
            String explanation,
            String sourceLabel,
            Long aiAssetId,
            LocalDateTime approvedAt
    ) {
        VerseExplanation verseExplanation = new VerseExplanation();
        verseExplanation.bibleVerseId = requirePositive(bibleVerseId, "bibleVerseId");
        verseExplanation.summary = requireText(summary, "summary");
        verseExplanation.explanation = requireText(explanation, "explanation");
        verseExplanation.sourceLabel = requireText(sourceLabel, "sourceLabel");
        verseExplanation.aiAssetId = requirePositive(aiAssetId, "aiAssetId");
        verseExplanation.approvedAt = Objects.requireNonNull(approvedAt, "approvedAt must not be null");
        verseExplanation.status = VerseExplanationStatus.APPROVED;
        verseExplanation.activeUniqueKey = ACTIVE_UNIQUE_KEY;
        return verseExplanation;
    }

    public void deactivate() {
        this.activeUniqueKey = null;
    }

    public void hide() {
        this.status = VerseExplanationStatus.HIDDEN;
        this.activeUniqueKey = null;
    }

    private static Long requirePositive(Long value, String fieldName) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException(fieldName + " must be positive");
        }
        return value;
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }
}
