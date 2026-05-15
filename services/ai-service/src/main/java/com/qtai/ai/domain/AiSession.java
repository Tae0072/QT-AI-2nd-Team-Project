package com.qtai.ai.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.time.LocalDate;

/**
 * AI 질문 세션. 02_ERD §4.3.
 *
 * <p>오늘 QT 본문(하루 1개, 범위 허용 — chapterStart/verseStart/chapterEnd/verseEnd + ordinal,
 * ADR-0021)에 대해 사용자별 N회 세션 가능. 각 세션은 1회성 Q&A.
 */
@Entity
@Table(name = "ai_sessions")
@Getter
@Setter
@NoArgsConstructor
public class AiSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "qt_date", nullable = false)
    private LocalDate qtDate;

    @Column(name = "book_code", length = 8, nullable = false)
    private String bookCode;

    @Column(nullable = false)
    private Integer chapter;

    @Column(name = "verse_start", nullable = false)
    private Integer verseStart;

    @Column(name = "verse_end", nullable = false)
    private Integer verseEnd;

    /** A · B · C · D — 큐티 가이드 단계 */
    @Column(name = "guide_step", length = 2)
    private String guideStep;

    /** IN_PROGRESS / COMPLETED / COMPLETED_NO_JOURNAL */
    @Column(length = 32, nullable = false)
    private String status = "IN_PROGRESS";

    @Column(columnDefinition = "TEXT")
    private String summary;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
