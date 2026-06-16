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
import com.qtai.domain.qt.client.sum.SuTodayPassage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 성서유니온 수집 본문 반영 + 절 매핑(qt_passage_verses) 저장의 코디네이터.
 *
 * <p>버그 수정(2026-06-05): 기존에는 qt_passages 행만 만들고 qt_passage_verses를
 * 채우는 코드가 없어, 자동 수집 본문은 AI 해설 시딩·학습 콘텐츠·시뮬레이터가
 * 쓰는 verseIds가 항상 비어 파이프라인이 통째로 끊겼다. 수집 시 bible api로
 * 절 범위를 verse id에 매핑해 함께 저장하고, 과거 누락분은 백필로 보강한다.
 *
 * <p>트랜잭션 설계: 이 메서드들은 <b>비트랜잭션 코디네이터</b>다. 절 범위 조회(bible
 * {@code getVerses}는 readOnly 트랜잭션이라 빈 장이면 예외를 던진다)를 쓰기 트랜잭션 <b>밖에서</b>
 * 수행해 예외를 격리하고({@link #collectRangeVersesOrEmpty}), 본문 저장과 매핑 저장은
 * {@link QtPassageWriter}가 각자의 트랜잭션으로 처리한다. 그래야 절 조회 실패가 본문 저장
 * 트랜잭션을 rollback-only로 오염시키지 않아 "절 매핑 실패해도 본문은 유지" 폴백이 보장된다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class QtTodayPassageImportService {

    private final QtPassageRepository qtPassageRepository;
    private final ListBibleBooksUseCase listBibleBooksUseCase;
    private final GetBibleVerseUseCase getBibleVerseUseCase;
    private final QtPassageWriter qtPassageWriter;

    public QtPassage importToday(LocalDate qtDate, SuTodayPassage passage) {
        BibleBookResponse book = findBookByEnglishName(passage.englishBookName());
        Short bookId = book.id().shortValue();

        // 1) 본문 저장(자체 트랜잭션, 커밋). 절 조회/매핑과 분리해 본문은 항상 보존한다.
        QtPassage saved = qtPassageWriter.upsert(qtDate, bookId, passage);

        // 2) 절 조회는 트랜잭션 밖에서 — 빈 장 예외를 격리한다(본문 저장 트랜잭션 오염 방지).
        List<BibleVerseResponse> verses = collectRangeVersesOrEmpty(
                book.code(), passage.chapter(), passage.endChapter(),
                passage.startVerse(), passage.endVerse());

        // 3) 미리 모은 절로 매핑 저장(자체 트랜잭션). 비었거나 실패하면 본문만 유지하고 백필 대상으로 남긴다.
        if (verses.isEmpty()) {
            log.warn("절 매핑 보류 — 본문만 저장, 백필 재시도 대상. qtDate={}, qtPassageId={}", qtDate, saved.getId());
        } else {
            try {
                qtPassageWriter.replaceMappings(saved.getId(), verses);
            } catch (RuntimeException exception) {
                log.warn("절 매핑 저장 실패 — 본문은 유지, 백필 재시도 대상. qtDate={}, qtPassageId={}, errorType={}, errorMessage={}",
                        qtDate, saved.getId(), exception.getClass().getSimpleName(), exception.getMessage());
            }
        }
        return saved;
    }

    /**
     * 절 매핑이 비어 있는 본문(과거 수집분 포함)을 일괄 보강한다.
     *
     * @return 매핑을 채운 본문 수
     */
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
            List<BibleVerseResponse> verses = collectRangeVersesOrEmpty(
                    book.code(), passage.getChapter(), passage.getEndChapter(),
                    passage.getStartVerse(), passage.getEndVerse());
            if (verses.isEmpty()) {
                continue;
            }
            try {
                if (qtPassageWriter.replaceMappings(passage.getId(), verses)) {
                    filledCount++;
                }
            } catch (RuntimeException exception) {
                log.warn("절 매핑 백필 실패 — 다음 본문 계속. qtPassageId={}, errorType={}, errorMessage={}",
                        passage.getId(), exception.getClass().getSimpleName(), exception.getMessage());
            }
        }
        return filledCount;
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
     * 시작~종료 장 범위의 절을 모은다. bible 조회 실패(빈 장 예외 등)는 잡아서 빈 목록을 돌려준다 —
     * 본문 저장 트랜잭션 밖에서 호출되므로 예외가 본문 저장을 롤백시키지 않는다.
     */
    private List<BibleVerseResponse> collectRangeVersesOrEmpty(String bookCode,
                                                               short startChapter, short endChapter,
                                                               short startVerse, short endVerse) {
        try {
            return collectRangeVerses(bookCode, startChapter, endChapter, startVerse, endVerse);
        } catch (RuntimeException exception) {
            log.warn("절 범위 조회 실패 — 매핑 보류. bookCode={}, range={}:{}~{}:{}, errorType={}, errorMessage={}",
                    bookCode, startChapter, startVerse, endChapter, endVerse,
                    exception.getClass().getSimpleName(), exception.getMessage());
            return List.of();
        }
    }

    /**
     * 시작~종료 장을 아우르는 절을 bible api로 모은다.
     *
     * <p>bible api {@code getVerses(bookCode, chapter, from, to)}는 단일 장 전용이므로,
     * 장 교차 범위는 장별로 조회하고 경계(시작 장의 시작 절 이전, 종료 장의 종료 절 이후)를
     * 필터링해 이어 붙인다. 중간 장은 장 전체({@code from=null})를 가져온다. 권 교차는 현재
     * 수집 소스에 없으므로 같은 권({@code bookCode}) 기준으로 처리한다. 한 장이라도 조회 결과나
     * 경계 필터 결과가 비면 부분 매핑을 저장하지 않고 빈 목록을 돌려 본문만 유지한다.
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
            if (chapterVerses.isEmpty()) {
                return List.of();
            }
            int collectedBeforeChapter = collected.size();
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
            if (collected.size() == collectedBeforeChapter) {
                return List.of();
            }
        }
        return collected;
    }
}
