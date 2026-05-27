package com.qtai.domain.bible.internal;

import com.qtai.config.JpaAuditingConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static com.qtai.support.TestEntityFactory.bibleBook;
import static com.qtai.support.TestEntityFactory.bibleVerse;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DataJpaTest
@Import(JpaAuditingConfig.class)
@ActiveProfiles("test")
class BibleRepositoryTest {

    @Autowired
    private BibleBookRepository bibleBookRepository;

    @Autowired
    private BibleRepository bibleRepository;

    @Test
    @DisplayName("성경 권 목록은 displayOrder 기준으로 정렬된다")
    void findAllByOrderByDisplayOrderAsc_returnsOrderedBooks() {
        BibleBook second = bibleBook((short) 2, "EXO", "출애굽기", "Exodus", (short) 2);
        BibleBook first = bibleBook((short) 1, "GEN", "창세기", "Genesis", (short) 1);
        bibleBookRepository.saveAll(List.of(second, first));

        List<BibleBook> books = bibleBookRepository.findAllByOrderByDisplayOrderAsc();

        assertThat(books).extracting(BibleBook::getCode).containsExactly("GEN", "EXO");
    }

    @Test
    @DisplayName("성경 절 좌표는 book_id, chapter_no, verse_no 조합으로 유일하다")
    void bibleVerseCoordinate_isUnique() {
        BibleBook book = bibleBookRepository.save(bibleBook((short) 1, "GEN", "창세기", "Genesis", (short) 1));
        bibleRepository.saveAndFlush(bibleVerse(book, (short) 1, (short) 1));

        assertThrows(Exception.class, () -> bibleRepository.saveAndFlush(bibleVerse(book, (short) 1, (short) 1)));
    }

    @Test
    @DisplayName("성경 절은 book과 chapter 기준으로 verseNo 오름차순 조회된다")
    void findByBookAndChapterNoOrderByVerseNoAsc_returnsOrderedVerses() {
        BibleBook book = bibleBookRepository.save(bibleBook((short) 1, "GEN", "창세기", "Genesis", (short) 1));
        bibleRepository.save(bibleVerse(book, (short) 1, (short) 2));
        bibleRepository.save(bibleVerse(book, (short) 1, (short) 1));
        bibleRepository.flush();

        List<BibleVerse> verses = bibleRepository.findByBookAndChapterNoOrderByVerseNoAsc(book, (short) 1);

        assertThat(verses).extracting(BibleVerse::getVerseNo).containsExactly((short) 1, (short) 2);
    }
}
