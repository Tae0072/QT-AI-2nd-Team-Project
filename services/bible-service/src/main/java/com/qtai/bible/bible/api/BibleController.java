package com.qtai.bible.bible.api;

import com.qtai.bible.bible.domain.Book;
import com.qtai.bible.bible.infrastructure.BookRepository;
import com.qtai.bible.bible.infrastructure.EnVerseRepository;
import com.qtai.bible.bible.infrastructure.KrVerseRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * 성경 본문 조회 API.
 *
 * <p>참조: apis/bible/openapi.yaml — GET /bible/{kr|en}/{bookCode}/{chapter}/{verse}, GET /bible/books
 * <p>캐시: cache:passage:kr|en:{bookCode}:{ch}:{v}, TTL 24h (DECISIONS.md §9.3)
 *
 * <p>TODO(이지윤·이승욱):
 * - Flyway V1: bible_books, bible_kr_verses, bible_en_verses 시드 (서울 교육 목적 출처 표기)
 * - Redis 캐시 TTL 24h
 * - 404 시 ProblemDetail RESOURCE_NOT_FOUND
 */
@RestController
@RequestMapping("/bible")
public class BibleController {

    private final BookRepository books;
    private final KrVerseRepository krVerses;
    private final EnVerseRepository enVerses;

    public BibleController(BookRepository books, KrVerseRepository krVerses, EnVerseRepository enVerses) {
        this.books = books;
        this.krVerses = krVerses;
        this.enVerses = enVerses;
    }

    @GetMapping("/books")
    @Cacheable(value = "bibleBooks")
    public Map<String, Object> listBooks() {
        List<Map<String, Object>> items = books.findAll().stream()
                .map(b -> Map.<String, Object>of(
                        "bookCode", b.getBookCode(),
                        "nameKr", b.getNameKr(),
                        "nameEn", b.getNameEn(),
                        "testament", b.getTestament(),
                        "ordinal", b.getOrdinal()))
                .toList();
        return Map.of("items", items);
    }

    @GetMapping("/kr/{bookCode}/{chapter}/{verse}")
    @Cacheable(value = "krVerse", key = "#bookCode + ':' + #chapter + ':' + #verse")
    public ResponseEntity<Map<String, Object>> krVerse(@PathVariable String bookCode,
                                                       @PathVariable Integer chapter,
                                                       @PathVariable Integer verse) {
        Book book = books.findByBookCode(bookCode)
                .orElseThrow(() -> new NoSuchElementException("Unknown bookCode: " + bookCode));
        var v = krVerses.findFirstByBookIdAndChapterAndVerseAndVersion(book.getId(), chapter, verse, "REVISED")
                .orElseThrow(() -> new NoSuchElementException("Verse not found"));
        return ResponseEntity.ok(Map.of(
                "bookCode", bookCode,
                "bookNameKr", book.getNameKr(),
                "chapter", chapter,
                "verse", verse,
                "content", v.getContent(),
                "version", v.getVersion(),
                "language", "ko"
        ));
    }

    @GetMapping("/en/{bookCode}/{chapter}/{verse}")
    @Cacheable(value = "enVerse", key = "#bookCode + ':' + #chapter + ':' + #verse")
    public ResponseEntity<Map<String, Object>> enVerse(@PathVariable String bookCode,
                                                       @PathVariable Integer chapter,
                                                       @PathVariable Integer verse) {
        Book book = books.findByBookCode(bookCode)
                .orElseThrow(() -> new NoSuchElementException("Unknown bookCode: " + bookCode));
        var v = enVerses.findFirstByBookIdAndChapterAndVerseAndVersion(book.getId(), chapter, verse, "KJV")
                .orElseThrow(() -> new NoSuchElementException("Verse not found"));
        return ResponseEntity.ok(Map.of(
                "bookCode", bookCode,
                "bookNameEn", book.getNameEn(),
                "chapter", chapter,
                "verse", verse,
                "content", v.getContent(),
                "version", v.getVersion(),
                "language", "en"
        ));
    }
}
