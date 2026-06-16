package com.qtai.domain.qt.internal;

import com.qtai.domain.bible.api.dto.BibleVerseResponse;
import com.qtai.domain.qt.api.QtPassageVerseMappingsChangedEvent;
import com.qtai.domain.qt.client.sum.SuTodayPassage;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * QT 본문(qt_passages)과 절 매핑(qt_passage_verses)을 저장하는 트랜잭션 경계.
 *
 * <p>본문 저장과 절 매핑 교체는 별도 트랜잭션으로 둔다. 수집 본문은 저장 즉시 노출하지 않고
 * PENDING_REVIEW로 예약한 뒤 04:00 자동게시가 게시 시각을 채운다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
class QtPassageWriter {

    private final QtPassageRepository qtPassageRepository;
    private final QtPassageVerseRepository qtPassageVerseRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;

    /**
     * 같은 날짜 본문이 있으면 갱신하고, 없으면 새로 생성한다.
     *
     * <p>수집 시각은 매 수집마다 갱신한다. 이미 게시된 본문을 재수집해도 게시 시각과 상태는 보존한다.
     */
    @Transactional
    public QtPassage upsert(LocalDate qtDate, Short bookId, SuTodayPassage passage) {
        QtPassage saved = qtPassageRepository.findByQtDate(qtDate)
                .map(existing -> updateExisting(existing, bookId, passage))
                .orElseGet(() -> createNew(qtDate, bookId, passage));
        saved.recordCollected(LocalDateTime.now(clock), null);
        return saved;
    }

    /**
     * 미리 조회한 절 목록으로 qt_passage_verses를 교체하고 매핑 변경 이벤트를 발행한다.
     *
     * @return 매핑 저장 여부. 절 목록이 비어 있으면 false를 반환하고 기존 매핑을 건드리지 않는다.
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
        log.info("QT 매핑 저장 완료. qtPassageId={}, verseCount={}", qtPassageId, mappings.size());
        eventPublisher.publishEvent(new QtPassageVerseMappingsChangedEvent(qtPassageId));
        return true;
    }

    private QtPassage createNew(LocalDate qtDate, Short bookId, SuTodayPassage passage) {
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
        qtPassage.scheduleForAutoPublish();
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
