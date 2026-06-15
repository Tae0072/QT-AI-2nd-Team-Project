package com.qtai.domain.qt.internal;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.qtai.domain.bible.api.dto.BibleVerseResponse;
import com.qtai.domain.qt.client.sum.SuTodayPassage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * QT 본문(qt_passages)과 절 매핑(qt_passage_verses)의 쓰기 트랜잭션 경계.
 *
 * <p>본문 저장과 절 매핑을 <b>별도 트랜잭션</b>으로 나눈다. 절 범위 조회 bible api는
 * {@code @Transactional(readOnly = true)}라 빈 장이면 예외를 던지는데, 이 조회를 본문 저장과
 * 같은 트랜잭션에서 하면 예외가 트랜잭션을 rollback-only로 만들어 본문 저장까지 롤백된다
 * ({@code UnexpectedRollbackException}). 따라서 호출자({@link QtTodayPassageImportService})는
 * 절 조회를 트랜잭션 <b>밖에서</b> 수행해 예외를 격리하고, 미리 모은 절만 이 빈에 넘긴다.
 * 본문은 {@link #upsert}로 먼저 커밋되고, 매핑은 {@link #replaceMappings}가 별도로 쓴다 —
 * 매핑이 실패해도 본문은 유지되고(백필 재시도 대상), 매핑은 이미 커밋된 본문을 FK로 안전하게 참조한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
class QtPassageWriter {

    private final QtPassageRepository qtPassageRepository;
    private final QtPassageVerseRepository qtPassageVerseRepository;

    /** 본문을 upsert 한다(같은 날짜가 있으면 갱신, 없으면 생성). 자체 트랜잭션에서 커밋된다. */
    @Transactional
    public QtPassage upsert(LocalDate qtDate, Short bookId, SuTodayPassage passage) {
        return qtPassageRepository.findByQtDate(qtDate)
                .map(existing -> updateExisting(existing, bookId, passage))
                .orElseGet(() -> createNew(qtDate, bookId, passage));
    }

    /**
     * 미리 조회한 절들로 qt_passage_verses를 교체 저장한다.
     * 본문이 이미 커밋돼 있어야 FK가 안전하다(호출자가 {@link #upsert} 이후 호출).
     *
     * @return 매핑 저장 여부 (verses가 비면 false — 매핑을 건드리지 않는다)
     */
    @Transactional
    public boolean replaceMappings(Long qtPassageId, List<BibleVerseResponse> verses) {
        if (verses == null || verses.isEmpty()) {
            return false;
        }
        qtPassageVerseRepository.deleteByQtPassageId(qtPassageId);
        short displayOrder = 1;
        List<QtPassageVerse> mappings = new ArrayList<>(verses.size());
        for (BibleVerseResponse verse : verses) {
            mappings.add(QtPassageVerse.create(qtPassageId, verse.id(), displayOrder++));
        }
        qtPassageVerseRepository.saveAll(mappings);
        log.info("절 매핑 저장 완료. qtPassageId={}, verseCount={}", qtPassageId, mappings.size());
        return true;
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
}
