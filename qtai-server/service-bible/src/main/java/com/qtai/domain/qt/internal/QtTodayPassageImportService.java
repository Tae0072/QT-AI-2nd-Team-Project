package com.qtai.domain.qt.internal;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.qtai.common.exception.BusinessException;
import com.qtai.common.exception.ErrorCode;
import com.qtai.domain.bible.api.GetBibleVerseUseCase;
import com.qtai.domain.bible.api.ListBibleBooksUseCase;
import com.qtai.domain.bible.api.dto.BibleBookResponse;
import com.qtai.domain.bible.api.dto.BibleVerseRangeResponse;
import com.qtai.domain.bible.api.dto.BibleVerseResponse;
import com.qtai.domain.qt.api.QtPassageVerseMappingsChangedEvent;
import com.qtai.domain.qt.client.sum.SuTodayPassage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 성서유니온 수집 본문 반영 + 절 매핑(qt_passage_verses) 저장.
 *
 * <p>버그 수정(2026-06-05): 기존에는 qt_passages 행만 만들고 qt_passage_verses를
 * 채우는 코드가 없어, 자동 수집 본문은 AI 해설 시딩·학습 콘텐츠·시뮬레이터가
 * 쓰는 verseIds가 항상 비어 파이프라인이 통째로 끊겼다. 수집 시 bible api로
 * 절 범위를 verse id에 매핑해 함께 저장하고, 과거 누락분은 백필로 보강한다.
 *
 * <p>책 식별도 bible_books 테이블 native 조인 대신 bible api(ListBibleBooksUseCase)
 * 경유로 교체 — 도메인 경계의 SQL 우회 제거(MSA 분리 대비).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class QtTodayPassageImportService {

    private final QtPassageRepository qtPassageRepository;
    private final QtPassageVerseRepository qtPassageVerseRepository;
    private final ListBibleBooksUseCase listBibleBooksUseCase;
    private final GetBibleVerseUseCase getBibleVerseUseCase;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public QtPassage importToday(LocalDate qtDate, SuTodayPassage passage) {
        BibleBookResponse book = findBookByEnglishName(passage.englishBookName());
        Short bookId = book.id().shortValue();

        QtPassage saved = qtPassageRepository.findByQtDate(qtDate)
                .map(qtPassage -> updateExisting(qtPassage, bookId, passage))
                .orElseGet(() -> createNew(qtDate, bookId, passage));

        // 절 매핑 실패는 본문 반영을 막지 않는다 — 본문(범위 기반 사용자 조회)은 유지하고
        // 매핑은 startup 백필이 재시도한다. (AI/학습 파이프라인 입력은 매핑에 의존)
        replaceVerseMappings(saved, book.code(), passage.chapter(), passage.endChapter(),
                passage.startVerse(), passage.endVerse());
        return saved;
    }

    /**
     * 절 매핑이 비어 있는 본문(과거 수집분 포함)을 일괄 보강한다.
     *
     * @return 매핑을 채운 본문 수
     */
    @Transactional
    public int backfillMissingVerseMappings() {
        List<QtPassage> targets = qtPassageRepository.findAllWithoutVerseMappings();
        if (targets.isEmpty()) {
            return 0;
        }
        Map<Short, BibleBookResponse> booksById = listBibleBooksUseCase.listBibleBooks().stream()
                .collect(Collectors.toMap(book -> book.id().shortValue(), Function.identity()));

        int filledCount = 0;
        for (QtPassage passage : targets) {
            BibleBookResponse book = booksById.get(passage.getBookId());
            if (book == null) {
                log.error("절 매핑 백필 실패 — bible_books에 없는 book_id. qtPassageId={}, bookId={}",
                        passage.getId(), passage.getBookId());
                continue;
            }
            boolean filled = replaceVerseMappings(
                    passage,
                    book.code(),
                    passage.getChapter(),
                    passage.getEndChapter(),
                    passage.getStartVerse(),
                    passage.getEndVerse()
            );
            if (filled) {
                filledCount++;
            }
        }
        return filledCount;
    }

    private QtPassage createNew(LocalDate qtDate, Short bookId, SuTodayPassage passage) {
        // 성서유니온 본문은 같은 권 안에서만 장이 교차한다 → 종료 권 = 시작 권.
        QtPassage qtPassage = QtPassage.create(
                qtDate,
                bookId,
                bookId,
                passage.chapter(),
                passage.endChapter(),
                passage.startVerse(),
                passage.endVerse(),
                passage.title(),
                passage.referenceText()
        );
        return qtPassageRepository.save(qtPassage);
    }

    private QtPassage updateExisting(QtPassage qtPassage, Short bookId, SuTodayPassage passage) {
        qtPassage.updateRange(
                bookId,
                bookId,
                passage.chapter(),
                passage.endChapter(),
                passage.startVerse(),
                passage.endVerse(),
                passage.title(),
                passage.referenceText()
        );
        return qtPassageRepository.save(qtPassage);
    }

    private BibleBookResponse findBookByEnglishName(String englishName) {
        String normalized = englishName == null ? "" : englishName.trim();
        return listBibleBooksUseCase.listBibleBooks().stream()
                .filter(book -> book.englishName().equalsIgnoreCase(normalized))
                .findFirst()
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.BIBLE_BOOK_NOT_FOUND,
                        "DB에 등록되지 않은 성경 권입니다. englishName=" + englishName));
    }

    /**
     * 본문의 절 범위를 bible api로 verse id에 매핑해 qt_passage_verses를 교체 저장한다.
     *
     * @return 매핑 저장 성공 여부 (실패는 로그만 남기고 본문 반영은 유지)
     */
    private boolean replaceVerseMappings(QtPassage qtPassage, String bookCode,
                                         short startChapter, short endChapter,
                                         short startVerse, short endVerse) {
        try {
            List<BibleVerseResponse> verses = collectRangeVerses(
                    bookCode, startChapter, endChapter, startVerse, endVerse);
            if (verses.isEmpty()) {
                log.error("절 매핑 실패 — bible 절 범위 조회 결과 없음. qtPassageId={}, bookCode={}, range={}:{}~{}:{}",
                        qtPassage.getId(), bookCode, startChapter, startVerse, endChapter, endVerse);
                return false;
            }

            qtPassageVerseRepository.deleteByQtPassageId(qtPassage.getId());
            short displayOrder = 1;
            List<QtPassageVerse> mappings = new ArrayList<>(verses.size());
            for (BibleVerseResponse verse : verses) {
                mappings.add(QtPassageVerse.create(qtPassage.getId(), verse.id(), displayOrder++));
            }
            qtPassageVerseRepository.saveAll(mappings);
            log.info("절 매핑 저장 완료. qtPassageId={}, verseCount={}", qtPassage.getId(), mappings.size());
            eventPublisher.publishEvent(new QtPassageVerseMappingsChangedEvent(qtPassage.getId()));
            return true;
        } catch (RuntimeException exception) {
            log.error("절 매핑 실패 — 본문은 유지, startup 백필 재시도 대상. qtPassageId={}, bookCode={}, errorType={}, errorMessage={}",
                    qtPassage.getId(), bookCode,
                    exception.getClass().getSimpleName(), exception.getMessage());
            return false;
        }
    }

    /**
     * 시작~종료 장을 아우르는 절을 bible api로 모은다.
     *
     * <p>bible api {@code getVerses(bookCode, chapter, from, to)}는 단일 장 전용이므로,
     * 장 교차 범위는 장별로 조회하고 경계(시작 장의 시작 절 이전, 종료 장의 종료 절 이후)를
     * 필터링해 이어 붙인다. 중간 장은 장 전체({@code from=null})를 가져온다. 권 교차는 현재
     * 수집 소스에 없으므로 같은 권({@code bookCode}) 기준으로 처리한다.
     */
    private List<BibleVerseResponse> collectRangeVerses(String bookCode,
                                                        short startChapter, short endChapter,
                                                        short startVerse, short endVerse) {
        if (startChapter == endChapter) {
            BibleVerseRangeResponse range = getBibleVerseUseCase.getVerses(
                    bookCode, startChapter, (int) startVerse, (int) endVerse);
            return range == null || range.verses() == null ? List.of() : range.verses();
        }

        List<BibleVerseResponse> collected = new ArrayList<>();
        for (int chapter = startChapter; chapter <= endChapter; chapter++) {
            BibleVerseRangeResponse range = getBibleVerseUseCase.getVerses(bookCode, chapter, null, null);
            List<BibleVerseResponse> chapterVerses =
                    range == null || range.verses() == null ? List.of() : range.verses();
            for (BibleVerseResponse verse : chapterVerses) {
                int verseNo = verse.verseNo() == null ? 0 : verse.verseNo();
                if (chapter == startChapter && verseNo < startVerse) {
                    continue;
                }
                if (chapter == endChapter && verseNo > endVerse) {
                    continue;
                }
                collected.add(verse);
            }
        }
        return collected;
    }
}
