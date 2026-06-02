package com.qtai.domain.qt.internal;

import com.qtai.common.exception.BusinessException;
import com.qtai.common.exception.ErrorCode;
import com.qtai.domain.note.api.GetNoteUseCase;
import com.qtai.domain.note.api.NoteCategory;
import com.qtai.domain.note.api.dto.NoteDraftResponse;
import com.qtai.domain.qt.api.GetQtPassageContentContextUseCase;
import com.qtai.domain.qt.api.GetTodayQtUseCase;
import com.qtai.domain.qt.api.dto.QtPassageContentContext;
import com.qtai.domain.qt.api.dto.TodayQtRangeResponse;
import com.qtai.domain.qt.api.dto.TodayQtResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * QT 도메인 서비스 — Today QT 조회 + Note 도메인 연동.
 *
 * <p>캐시된 공용 본문 데이터(QtPassageLookup)에 사용자별 데이터(draftNoteId)를
 * enrich하여 반환한다.
 *
 * <p>캐시 분리 구조:
 * <ul>
 *   <li>{@link QtPassageLookup} — 날짜 키 캐시, 공용 데이터(draftNoteId=null)</li>
 *   <li>{@code QtService} — 캐시 바깥에서 memberId로 note 도메인 조회 후 enrich</li>
 * </ul>
 *
 * <p>타 도메인 접근:
 * <ul>
 *   <li>note.api.GetNoteUseCase — MEDITATION 카테고리 DRAFT 노트 조회</li>
 * </ul>
 *
 * @see QtPassageLookup
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class QtService implements GetTodayQtUseCase, GetQtPassageContentContextUseCase {

    private final QtPassageLookup passageLookup;
    private final QtPassageRepository qtPassageRepository;
    private final QtPassageVerseRepository qtPassageVerseRepository;
    private final GetNoteUseCase getNoteUseCase;

    // ------------------------------------------------------------------
    // GetTodayQtUseCase 구현
    // ------------------------------------------------------------------

    /**
     * 오늘의 QT 본문 통합 응답을 반환한다.
     *
     * <p>흐름:
     * <ol>
     *   <li>{@link QtPassageLookup#findTodayPassage()}로 캐시된 공용 본문 조회</li>
     *   <li>본문이 있으면(qtPassageId != null) note 도메인에서 DRAFT 노트 ID 조회</li>
     *   <li>draftNoteId를 enrich한 응답 반환</li>
     * </ol>
     *
     * @param memberId 인증된 사용자 ID (DRAFT 노트 조회에 사용)
     */
    @Override
    public TodayQtResponse getToday(Long memberId) {
        TodayQtResponse base = passageLookup.findTodayPassage();
        Long draftNoteId = resolveDraftNoteId(memberId, base.qtPassageId());
        return enrichWithDraftNoteId(base, draftNoteId);
    }

    /**
     * 특정 QT 본문을 ID로 조회한다.
     *
     * @param memberId    인증된 사용자 ID (DRAFT 노트 조회에 사용)
     * @param qtPassageId QT 본문 식별자
     * @return QT 본문 통합 응답
     * @throws BusinessException QT_PASSAGE_NOT_FOUND — 본문이 존재하지 않을 때
     */
    @Override
    public TodayQtResponse getPassage(Long memberId, Long qtPassageId) {
        QtPassage passage = qtPassageRepository.findById(qtPassageId)
                .orElseThrow(() -> new BusinessException(ErrorCode.QT_PASSAGE_NOT_FOUND));

        Long draftNoteId = resolveDraftNoteId(memberId, qtPassageId);
        return new TodayQtResponse(
                passage.getId(),
                passage.getQtDate().toString(),
                passage.getTitle(),
                "MISSING",    // simulatorStatus: 시뮬레이터 도메인 연동 전 기본값
                false,        // hasExplanation: AI 해설 도메인 연동 전 기본값
                draftNoteId,
                "HIT",
                resolveRange(passage)
        );
    }

    @Override
    public QtPassageContentContext getContentContext(Long qtPassageId) {
        if (qtPassageId == null || qtPassageId < 1) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }

        QtPassage passage = qtPassageRepository.findById(qtPassageId)
                .orElseThrow(() -> new BusinessException(ErrorCode.QT_PASSAGE_NOT_FOUND));
        List<Long> verseIds = qtPassageVerseRepository.findByQtPassageIdOrderByDisplayOrderAsc(qtPassageId)
                .stream()
                .map(QtPassageVerse::getBibleVerseId)
                .toList();

        return new QtPassageContentContext(
                passage.getId(),
                passage.getQtDate(),
                passage.getTitle(),
                verseIds,
                true
        );
    }

    // ------------------------------------------------------------------
    // Private helpers
    // ------------------------------------------------------------------

    /**
     * note 도메인에서 해당 사용자의 MEDITATION DRAFT 노트 ID를 조회한다.
     *
     * <p>memberId 또는 qtPassageId가 null이면 노트 조회를 생략하고 null을 반환한다.
     * note 도메인 호출 중 예외가 발생해도 QT 응답 전체가 실패하지 않도록
     * null로 fallback한다.
     *
     * @param memberId    인증된 사용자 ID
     * @param qtPassageId QT 본문 ID
     * @return DRAFT 노트 ID, 없으면 null
     */
    private Long resolveDraftNoteId(Long memberId, Long qtPassageId) {
        if (memberId == null || qtPassageId == null) {
            return null;
        }
        try {
            NoteDraftResponse draft = getNoteUseCase.getDraft(
                    memberId, NoteCategory.MEDITATION, qtPassageId);
            return draft.exists() ? draft.note().id() : null;
        } catch (Exception e) {
            log.warn("DRAFT 노트 조회 실패. memberId={}, qtPassageId={}, error={}",
                    memberId, qtPassageId, e.getMessage());
            return null;
        }
    }

    /**
     * 캐시된 공용 응답에 사용자별 draftNoteId를 enrich한 새 응답을 반환한다.
     */
    private TodayQtResponse enrichWithDraftNoteId(TodayQtResponse base, Long draftNoteId) {
        return new TodayQtResponse(
                base.qtPassageId(),
                base.passageDate(),
                base.title(),
                base.simulatorStatus(),
                base.hasExplanation(),
                draftNoteId,
                base.cacheStatus(),
                base.range()
        );
    }

    private TodayQtRangeResponse resolveRange(QtPassage passage) {
        var range = qtPassageRepository.findRangeByQtPassageId(passage.getId());
        if (range == null) {
            return null;
        }
        return range
                .map(TodayQtRangeMapper::toResponse)
                .orElse(null);
    }
}
