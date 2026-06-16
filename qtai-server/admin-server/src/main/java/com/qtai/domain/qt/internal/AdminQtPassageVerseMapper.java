package com.qtai.domain.qt.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.qtai.domain.bible.api.GetBibleVerseUseCase;
import com.qtai.domain.bible.api.ListBibleBooksUseCase;
import com.qtai.domain.bible.api.dto.BibleBookResponse;
import com.qtai.domain.bible.api.dto.BibleVerseRangeResponse;
import com.qtai.domain.bible.api.dto.BibleVerseResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 관리자 QT 본문 등록/수정 시 절 매핑(qt_passage_verses)을 채우는 코디네이터.
 *
 * <p>배경: 기존에는 자동수집(QtTodayPassageImportService)만 절 매핑을 채워서, 관리자가 직접 등록한
 * 오늘 QT는 verseIds가 비어 QT영상 클립 준비가 "no verse mapping"으로 스킵(MISSING)됐다. 이 빈은
 * 관리자 등록 경로에도 자동수집과 동등하게 절 매핑을 채운다. 매핑은 이미 DB에 있는 성경 절을
 * 범위로 조회해 verse id를 연결하는 작업이라 외부 호출이 필요 없다(bible 조회는 admin-server 내부 도메인).
 *
 * <p>트랜잭션 설계(중요):
 * <ul>
 *   <li>bible {@code getVerses}는 {@code @Transactional(readOnly=true)}라 빈 장이면 예외를 던지는데, 이를
 *       본문 저장과 같은 트랜잭션에서 호출하면 rollback-only가 되어 본문 저장까지 롤백된다. 따라서 매핑은
 *       본문 저장 트랜잭션 <b>커밋 이후</b>({@link #mapVersesAfterCommit})에 수행한다.</li>
 *   <li>절 조회는 커밋 후(활성 트랜잭션 없음) 각자의 read 트랜잭션으로 돌려 예외를 격리한다.</li>
 *   <li>매핑 쓰기({@link #persistTx})는 <b>프로그래매틱 REQUIRES_NEW</b> 트랜잭션으로 커밋한다.
 *       {@code afterCommit} 단계에서 어노테이션 {@code @Transactional}(REQUIRED) 쓰기는 완료 중인
 *       트랜잭션 리소스에 묶여 커밋되지 않는 Spring의 함정이 있어, 새 트랜잭션을 명시적으로 연다.</li>
 * </ul>
 * 매핑 실패는 best-effort로 로그만 남기고 등록 요청을 실패시키지 않는다.
 *
 * <p>이벤트는 발행하지 않는다 — QT영상 클립 소비자는 service-bible에 있고 admin-server와 프로세스가
 * 분리돼 있어 인-프로세스 이벤트가 닿지 않는다. 클립 준비는 service-bible의 일일 스케줄러가
 * 공유 DB의 매핑을 읽어 수행한다(2b).
 */
@Slf4j
@Component
public class AdminQtPassageVerseMapper {

    private final ListBibleBooksUseCase listBibleBooksUseCase;
    private final GetBibleVerseUseCase getBibleVerseUseCase;
    private final QtPassageWriter qtPassageWriter;
    /** 매핑 쓰기를 커밋하기 위한 REQUIRES_NEW 트랜잭션 템플릿(afterCommit 커밋 함정 회피). */
    private final TransactionTemplate persistTx;

    public AdminQtPassageVerseMapper(ListBibleBooksUseCase listBibleBooksUseCase,
                                     GetBibleVerseUseCase getBibleVerseUseCase,
                                     QtPassageWriter qtPassageWriter,
                                     PlatformTransactionManager transactionManager) {
        this.listBibleBooksUseCase = listBibleBooksUseCase;
        this.getBibleVerseUseCase = getBibleVerseUseCase;
        this.qtPassageWriter = qtPassageWriter;
        this.persistTx = new TransactionTemplate(transactionManager);
        this.persistTx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    /**
     * 현재 쓰기 트랜잭션이 커밋된 뒤 절 매핑을 채운다. 트랜잭션이 없으면(테스트 등) 즉시 수행한다.
     * 호출자({@link AdminQtPassageService})의 {@code @Transactional} create/update 안에서 호출한다.
     */
    public void mapVersesAfterCommit(Long qtPassageId, Short bookId,
                                     short startChapter, short endChapter,
                                     short startVerse, short endVerse) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            mapVerses(qtPassageId, bookId, startChapter, endChapter, startVerse, endVerse);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                mapVerses(qtPassageId, bookId, startChapter, endChapter, startVerse, endVerse);
            }
        });
    }

    /**
     * 본문 범위의 절을 조회해 qt_passage_verses를 교체 저장한다. 절 조회는 read 트랜잭션(예외 격리),
     * 쓰기는 REQUIRES_NEW 트랜잭션으로 커밋한다. 모든 실패는 best-effort로 로그만 남긴다.
     */
    public void mapVerses(Long qtPassageId, Short bookId,
                          short startChapter, short endChapter,
                          short startVerse, short endVerse) {
        try {
            BibleBookResponse book = findBookById(bookId);
            if (book == null) {
                log.warn("절 매핑 보류 — bible_books에 없는 book_id. qtPassageId={}, bookId={}", qtPassageId, bookId);
                return;
            }
            List<BibleVerseResponse> verses = collectRangeVersesOrEmpty(
                    book.code(), startChapter, endChapter, startVerse, endVerse);
            if (verses.isEmpty()) {
                log.warn("절 매핑 보류 — 조회 결과 없음(백필 재시도 대상). qtPassageId={}, bookCode={}, range={}:{}~{}:{}",
                        qtPassageId, book.code(), startChapter, startVerse, endChapter, endVerse);
                return;
            }
            persistTx.executeWithoutResult(status -> qtPassageWriter.replaceMappings(qtPassageId, verses));
        } catch (RuntimeException exception) {
            log.warn("절 매핑 실패 — 본문은 유지, 백필 재시도 대상. qtPassageId={}, errorType={}, errorMessage={}",
                    qtPassageId, exception.getClass().getSimpleName(), exception.getMessage());
        }
    }

    private BibleBookResponse findBookById(Short bookId) {
        if (bookId == null) {
            return null;
        }
        Map<Short, BibleBookResponse> booksById = listBibleBooksUseCase.listBibleBooks().stream()
                .collect(Collectors.toMap(book -> book.id().shortValue(), Function.identity(), (a, b) -> a));
        return booksById.get(bookId);
    }

    /**
     * 시작~종료 장 범위의 절을 모은다. bible 조회 실패(빈 장 예외 등)는 잡아서 빈 목록을 돌려준다.
     * 자동수집({@code QtTodayPassageImportService})의 장 교차 처리와 동일한 규약을 따른다.
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
     * 시작~종료 장을 아우르는 절을 bible api로 모은다. bible {@code getVerses}는 단일 장 전용이라,
     * 장 교차 범위는 장별로 조회하고 경계(시작 장의 시작 절 이전, 종료 장의 종료 절 이후)를 필터링해 잇는다.
     * 중간 장은 장 전체({@code verseFrom=null})를 가져온다. 한 장이라도 결과가 비면 부분 매핑을 막기 위해
     * 빈 목록을 돌린다.
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
