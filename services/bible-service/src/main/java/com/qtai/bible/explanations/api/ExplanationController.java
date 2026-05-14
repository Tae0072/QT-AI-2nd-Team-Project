package com.qtai.bible.explanations.api;

import com.qtai.bible.bible.domain.BibleExplanation;
import com.qtai.bible.bible.domain.Book;
import com.qtai.bible.bible.infrastructure.BibleExplanationRepository;
import com.qtai.bible.bible.infrastructure.BookRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * 본문 설명·해설 API.
 *
 * <p>경로 v2.0 (2026-05-14 리네이밍):
 * - GET /api/v1/explanations/{bookCode}/{ch}/{v}             — 쉬운 본문 설명 (요약/배경/어려운 단어)
 * - GET /api/v1/explanations/commentary/{bookCode}/{ch}/{v}  — 해설 (구 commentary) 목록
 *
 * <p>해설은 sourceType=GENERATED_EXPLANATION + editor_verified_at IS NOT NULL row만 반환.
 * 범위 포함 매칭(예: Genesis 41:37-57 row가 41:40 요청에 응답)을 지원한다.
 * REFERENCE_SOURCE(Tyndale/MHC 원천)는 절대 노출하지 않는다 — AI 컨텍스트 적재 전용.
 *
 * <p>빈 배열 가능 — 404 아님.
 *
 * <p>TODO(이지윤·이승욱):
 * - 별도 passage_explanation 테이블 분리 (summary, background, terms JSON)
 */
@RestController
@RequestMapping("/api/v1/explanations")
public class ExplanationController {

    private final BookRepository books;
    private final BibleExplanationRepository explanations;

    public ExplanationController(BookRepository books, BibleExplanationRepository explanations) {
        this.books = books;
        this.explanations = explanations;
    }

    @GetMapping("/{bookCode}/{chapter}/{verse}")
    @Cacheable(value = "passageExplanation", key = "#bookCode + ':' + #chapter + ':' + #verse")
    public Map<String, Object> passage(@PathVariable String bookCode,
                                       @PathVariable Integer chapter,
                                       @PathVariable Integer verse) {
        books.findByBookCode(bookCode)
                .orElseThrow(() -> new NoSuchElementException("Unknown bookCode"));
        // TODO: 별도 passage_explanation 테이블/엔티티 분리. 우선 더미 응답.
        return Map.of(
                "bookCode", bookCode,
                "chapter", chapter,
                "verse", verse,
                "summary", "본문 한 줄 요약 (TODO: DB)",
                "background", "본문 배경 설명 (TODO: DB)",
                "terms", List.of(Map.of("term", "TODO", "meaning", "TODO 어려운 단어 풀이"))
        );
    }

    @GetMapping("/commentary/{bookCode}/{chapter}/{verse}")
    @Cacheable(value = "explanationCommentary", key = "#bookCode + ':' + #chapter + ':' + #verse")
    public ResponseEntity<Map<String, Object>> commentary(@PathVariable String bookCode,
                                                          @PathVariable Integer chapter,
                                                          @PathVariable Integer verse) {
        Book book = books.findByBookCode(bookCode)
                .orElseThrow(() -> new NoSuchElementException("Unknown bookCode"));

        // GENERATED_EXPLANATION + editor_verified만 노출 (REFERENCE_SOURCE는 절대 노출 X)
        List<BibleExplanation> rows = explanations.findPublishedForVerse(book.getId(), chapter, verse);

        List<Map<String, Object>> items = rows.stream()
                .map(e -> Map.<String, Object>of(
                        "id", e.getId(),
                        "source", e.getSource(),
                        "language", e.getLanguage(),
                        "title", e.getTitle() == null ? "" : e.getTitle(),
                        "content", e.getContent(),
                        // 범위 정보도 함께 노출 — 화면에서 "Genesis 41:37-57" 같이 표시 가능
                        "range", Map.of(
                                "chapterStart", e.getChapterStart(),
                                "verseStart", e.getVerseStart(),
                                "chapterEnd", e.getChapterEnd(),
                                "verseEnd", e.getVerseEnd()
                        )))
                .toList();

        return ResponseEntity.ok(Map.of(
                "bookCode", bookCode,
                "chapter", chapter,
                "verse", verse,
                "items", items
        ));
    }
}
