package com.qtai.domain.study.internal;

import com.qtai.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(
        name = "glossary_terms",
        indexes = {
                @Index(name = "idx_glossary_terms_verse_status", columnList = "bible_verse_id, status"),
                @Index(name = "idx_glossary_terms_status", columnList = "status")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GlossaryTerm extends BaseEntity {

    @Column(name = "bible_verse_id", nullable = false)
    private Long bibleVerseId;

    @Column(nullable = false, length = 100)
    private String term;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String meaning;

    @Column(name = "source_label", nullable = false, length = 200)
    private String sourceLabel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private GlossaryTermStatus status = GlossaryTermStatus.HIDDEN;

    @Column(name = "ai_asset_id")
    private Long aiAssetId;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    public static GlossaryTerm approvedFromAiAsset(
            Long bibleVerseId,
            String term,
            String meaning,
            String sourceLabel,
            Long aiAssetId,
            LocalDateTime approvedAt
    ) {
        GlossaryTerm glossaryTerm = new GlossaryTerm();
        glossaryTerm.bibleVerseId = requirePositive(bibleVerseId, "bibleVerseId");
        glossaryTerm.term = requireText(term, "term");
        glossaryTerm.meaning = requireText(meaning, "meaning");
        glossaryTerm.sourceLabel = requireText(sourceLabel, "sourceLabel");
        glossaryTerm.aiAssetId = requirePositive(aiAssetId, "aiAssetId");
        glossaryTerm.approvedAt = Objects.requireNonNull(approvedAt, "approvedAt must not be null");
        glossaryTerm.status = GlossaryTermStatus.APPROVED;
        return glossaryTerm;
    }

    public void hide() {
        this.status = GlossaryTermStatus.HIDDEN;
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
