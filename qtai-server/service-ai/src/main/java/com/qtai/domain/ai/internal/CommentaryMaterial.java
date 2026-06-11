package com.qtai.domain.ai.internal;

import java.time.OffsetDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
        name = "commentary_materials",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_commentary_material_source_external",
                columnNames = {"source_id", "external_id"}
        )
)
public class CommentaryMaterial {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "source_id", nullable = false)
    private CommentarySource source;

    @Column(name = "external_id", nullable = false, length = 200)
    private String externalId;

    @Column(name = "material_type", nullable = false, length = 30)
    private String materialType;

    @Column(nullable = false, length = 100)
    private String refs;

    @Column(name = "book_code", nullable = false, length = 20)
    private String bookCode;

    @Column(name = "chapter_start", nullable = false)
    private Integer chapterStart;

    @Column(name = "verse_start", nullable = false)
    private Integer verseStart;

    @Column(name = "chapter_end", nullable = false)
    private Integer chapterEnd;

    @Column(name = "verse_end", nullable = false)
    private Integer verseEnd;

    @Column(length = 200)
    private String title;

    @Column(name = "keywords_json", columnDefinition = "JSON")
    private String keywordsJson;

    @Column(name = "content_text", nullable = false, columnDefinition = "TEXT")
    private String contentText;

    @Column(name = "content_html", columnDefinition = "TEXT")
    private String contentHtml;

    @Column(name = "content_hash", nullable = false, length = 100)
    private String contentHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CommentaryMaterialStatus status;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    protected CommentaryMaterial() {
    }

    public Long getId() {
        return id;
    }

    public CommentarySource getSource() {
        return source;
    }

    public String getRefs() {
        return refs;
    }

    public String getBookCode() {
        return bookCode;
    }

    public Integer getChapterStart() {
        return chapterStart;
    }

    public Integer getVerseStart() {
        return verseStart;
    }

    public Integer getChapterEnd() {
        return chapterEnd;
    }

    public Integer getVerseEnd() {
        return verseEnd;
    }

    public String getTitle() {
        return title;
    }

    public String getContentText() {
        return contentText;
    }

    public CommentaryMaterialStatus getStatus() {
        return status;
    }
}
