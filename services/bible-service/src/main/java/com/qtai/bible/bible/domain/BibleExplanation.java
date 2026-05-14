package com.qtai.bible.bible.domain;

import com.qtai.bible.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * 해설 (구 COMMENTARIES → bible_explanations, 2026-05-14 리네이밍). 02_ERD §3.5.
 *
 * <p>두 종류 row를 함께 보관한다:
 * <ul>
 *   <li><b>REFERENCE_SOURCE</b> — Tyndale / Matthew Henry / Bible Dictionary 등 원천 참고 자료.
 *       AI 비교 데이터로만 사용 (AI 생성 시 컨텍스트로 적재). 사용자 응답에는 노출하지 않는다.</li>
 *   <li><b>GENERATED_EXPLANATION</b> — AI가 한국어로 만든 오늘 QT 해설.
 *       편집자 에이전트 검증(editor_verified_at NOT NULL)을 통과한 row만 사용자에게 노출된다.</li>
 * </ul>
 *
 * <p>구절 범위(chapter_start / verse_start ~ chapter_end / verse_end):
 * Genesis 41:37-57, Genesis 41:1-45, Genesis 41 전체 등 Tyndale/MHC 원천이 범위로 들어오기 때문.
 * 단일 절 row는 chapter_start == chapter_end, verse_start == verse_end 로 저장한다.
 *
 * <p>조회 시에는 범위 포함 조건(chapter/verse가 [start, end] 안에 들어가는지)으로 SELECT 한다.
 */
@Entity
@Table(name = "bible_explanations",
        indexes = {
                @Index(name = "idx_expl_passage_range",
                        columnList = "book_id, chapter_start, verse_start, chapter_end, verse_end")
        })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BibleExplanation extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "book_id", nullable = false)
    private Long bookId;

    // ---- 범위 (단일 절은 start == end) ----
    @Column(name = "chapter_start", nullable = false)
    private Integer chapterStart;

    @Column(name = "verse_start", nullable = false)
    private Integer verseStart;

    @Column(name = "chapter_end", nullable = false)
    private Integer chapterEnd;

    @Column(name = "verse_end", nullable = false)
    private Integer verseEnd;

    /** REFERENCE_SOURCE / GENERATED_EXPLANATION */
    @Column(name = "source_type", length = 32, nullable = false)
    private String sourceType;

    /** TYNDALE / MATTHEW_HENRY / BIBLE_DICTIONARY / AI_QT_KO / DUMMY_KR 등 */
    @Column(length = 32, nullable = false)
    private String source;

    @Column(length = 5, nullable = false)
    private String language; // ko / en

    @Column(length = 200)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    /** AI 생성 row만 의미가 있다. REFERENCE_SOURCE는 항상 null로 둔다. */
    @Column(name = "editor_verified_at")
    private Instant editorVerifiedAt;
}
