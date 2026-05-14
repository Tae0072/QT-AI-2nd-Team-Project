package com.qtai.ai.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

/**
 * AI 대화 턴. 02_ERD §4.4.
 *
 * <p>role: SYSTEM / USER / ASSISTANT
 * <p>sources(구 rag_sources, 2026-05-14 리네이밍): JSON 배열, 본문 해설 row 참조.
 */
@Entity
@Table(name = "ai_turns")
@Getter
@Setter
@NoArgsConstructor
public class AiTurn {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "session_id", nullable = false)
    private Long sessionId;

    @Column(length = 16, nullable = false)
    private String role;       // SYSTEM / USER / ASSISTANT

    @Column(name = "guide_step", length = 2)
    private String guideStep;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "content_redacted", nullable = false)
    private boolean contentRedacted = false;

    @Column(name = "prompt_template_id")
    private Long promptTemplateId;

    /** sources JSON: [{"type":"explanation","id":11,"language":"EN"}] */
    @Column(columnDefinition = "JSON")
    private String sources;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
