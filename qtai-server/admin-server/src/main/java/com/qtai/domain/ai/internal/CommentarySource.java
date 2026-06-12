package com.qtai.domain.ai.internal;

import java.time.OffsetDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
        name = "commentary_sources",
        uniqueConstraints = @UniqueConstraint(name = "uk_commentary_sources_key", columnNames = "source_key")
)
public class CommentarySource {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "source_key", nullable = false, length = 80)
    private String sourceKey;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(name = "source_label", nullable = false, length = 200)
    private String sourceLabel;

    @Column(length = 100)
    private String product;

    @Column(nullable = false, length = 10)
    private String language;

    @Column(name = "usage_type", nullable = false, length = 30)
    private String usageType;

    @Column(name = "license_label", length = 100)
    private String licenseLabel;

    @Column(name = "copyright_notice", columnDefinition = "TEXT")
    private String copyrightNotice;

    @Column(columnDefinition = "TEXT")
    private String attribution;

    @Column(name = "access_level", nullable = false, length = 30)
    private String accessLevel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CommentarySourceStatus status;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    protected CommentarySource() {
    }

    public Long getId() {
        return id;
    }

    public String getSourceKey() {
        return sourceKey;
    }

    public String getName() {
        return name;
    }

    public String getSourceLabel() {
        return sourceLabel;
    }

    public String getLicenseLabel() {
        return licenseLabel;
    }

    public String getCopyrightNotice() {
        return copyrightNotice;
    }

    public String getAttribution() {
        return attribution;
    }

    public CommentarySourceStatus getStatus() {
        return status;
    }
}
