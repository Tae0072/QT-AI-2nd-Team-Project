package com.qtai.domain.ai.internal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
        name = "commentary_material_verses",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_commentary_material_verse",
                columnNames = {"commentary_material_id", "bible_verse_id"}
        )
)
public class CommentaryMaterialVerse {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "commentary_material_id", nullable = false)
    private CommentaryMaterial material;

    @Column(name = "bible_verse_id", nullable = false)
    private Long bibleVerseId;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder;

    @Column(name = "match_type", nullable = false, length = 20)
    private String matchType;

    protected CommentaryMaterialVerse() {
    }

    public CommentaryMaterial getMaterial() {
        return material;
    }

    public Long getBibleVerseId() {
        return bibleVerseId;
    }

    public Integer getDisplayOrder() {
        return displayOrder;
    }
}
