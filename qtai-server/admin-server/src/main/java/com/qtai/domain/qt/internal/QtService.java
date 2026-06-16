package com.qtai.domain.qt.internal;

import com.qtai.common.exception.BusinessException;
import com.qtai.common.exception.ErrorCode;
import com.qtai.domain.note.api.GetNoteUseCase;
import com.qtai.domain.note.api.NoteCategory;
import com.qtai.domain.note.api.dto.NoteDraftResponse;
import com.qtai.domain.qt.api.GetQtPassageContentContextUseCase;
import com.qtai.domain.qt.api.GetTodayQtUseCase;
import com.qtai.domain.qt.api.dto.QtPassageContentContext;
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
    private final TodayQtRangeResolver rangeResolver;
    private final GetNoteUseCase getNoteUseCase;
    private final com.qtai.domain.study.api.GetQtStudyAvailabilityUseCase getQtStudyAvailabilityUseCase;
    private final java.time.Clock clock;

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
        // 시뮬레이터 상태·해설 진입점은 승인 시점에 바뀌므로 캐시(todayQt) 밖에서 enrich한다
        return enrichWithStudyAvailability(enrichWithDraftNoteId(base, draftNoteId));
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

        // 공개 게이트(CLAUDE.md §6) — QT 범위 공개는 해당일 00:00 KST.
        // 선등록된 미래 본문은 id 순회로도 열람 불가(존재 은닉을 위해 404).
        // 관리자 게시 상태가 ACTIVE이고 공개일이 지난 QT만 사용자에게 노출한다.
        if (!isVisibleToUsers(passage)) {
            throw new BusinessException(ErrorCode.QT_PASSAGE_NOT_FOUND);
        }

        Long draftNoteId = resolveDraftNoteId(memberId, qtPassageId);
        TodayQtResponse base = new TodayQtResponse(
                passage.getId(),
                passage.getQtDate().toString(),
                passage.getTitle(),
                "MISSING",    // simulatorStatus 기본값 — study 연동 실패 시 fallback
                false,        // hasExplanation 기본값 — study 연동 실패 시 fallback
                draftNoteId,
                "HIT",
                rangeResolver.resolve(passage)
        );
        return enrichWithStudyAvailability(base);
    }

    @Override
    public QtPassageContentContext getContentContext(Long qtPassageId) {
        if (qtPassageId == null || qtPassageId < 1) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }

        QtPassage passage = qtPassageRepository.findById(qtPassageId)
                .orElseThrow(() -> new BusinessException(ErrorCode.QT_PASSAGE_NOT_FOUND));
        return toContentContext(passage);
    }

    /**
     * 특정 날짜 본문의 콘텐츠 컨텍스트 조회 — 내부 배치 전용.
     *
     * <p>사용자 노출 정책(00:00~04:00 STALE_FALLBACK, 캐시)을 거치지 않고
     * qt_date로 직접 조회한다. 00:05 해설 시딩이 "어제 본문"을 시딩하던
     * 버그의 수정 경로 (CLAUDE.md §6).
     */
    @Override
    public java.util.Optional<QtPassageContentContext> findContentContextByDate(java.time.LocalDate qtDate) {
        if (qtDate == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
        return qtPassageRepository.findByQtDate(qtDate).map(this::toContentContext);
    }

    private QtPassageContentContext toContentContext(QtPassage passage) {
        List<Long> verseIds = qtPassageVerseRepository.findByQtPassageIdOrderByDisplayOrderAsc(passage.getId())
                .stream()
                .map(QtPassageVerse::getBibleVerseId)
                .toList();

        // 공개 게이트(CLAUDE.md §6) — 기존 published=true 하드코딩은 선등록 미래 본문의
        // 승인 해설·시뮬레이터 클립이 study 경로로 새는 구멍이었다. study 서비스들은
        // 이 플래그로 노출을 차단한다. (ai 사전 생성 경로는 published를 보지 않으므로
        // 관리자 선생성 워크플로우는 막히지 않는다)
        boolean published = isVisibleToUsers(passage);

        return new QtPassageContentContext(
                passage.getId(),
                passage.getQtDate(),
                passage.getTitle(),
                verseIds,
                published
        );
    }

    private boolean isVisibleToUsers(QtPassage passage) {
        return passage.getStatus() == QtPassageStatus.ACTIVE
                && !passage.getQtDate().isAfter(java.time.LocalDate.now(clock));
    }

    // ------------------------------------------------------------------
    // Private helpers
    // ------------------------------------------------------------------

    /**
     * study 도메인에서 시뮬레이터 상태·승인 해설 존재 여부를 조회해 응답을 보강한다.
     *
     * <p>"Today QT 100%"(CLAUDE.md §6) — 기존에는 두 값이 하드코딩이라
     * 콘텐츠가 승인돼도 클라이언트 버튼이 영구 비활성이었다.
     * study 호출 실패 시 응답 전체가 실패하지 않도록 기본값(MISSING/false)으로 fallback.
     */
    private TodayQtResponse enrichWithStudyAvailability(TodayQtResponse base) {
        if (base.qtPassageId() == null) {
            return base;
        }
        try {
            List<Long> verseIds = qtPassageVerseRepository
                    .findByQtPassageIdOrderByDisplayOrderAsc(base.qtPassageId())
                    .stream()
                    .map(QtPassageVerse::getBibleVerseId)
                    .toList();
            var availability = getQtStudyAvailabilityUseCase.getAvailability(base.qtPassageId(), verseIds);
            if (availability == null) {
                return base;
            }
            return new TodayQtResponse(
                    base.qtPassageId(),
                    base.passageDate(),
                    base.title(),
                    availability.simulatorStatus(),
                    availability.hasExplanation(),
                    base.draftNoteId(),
                    base.cacheStatus(),
                    base.range()
            );
        } catch (RuntimeException exception) {
            log.warn("study 가용성 조회 실패 — 기본값(MISSING/false)으로 응답. qtPassageId={}, errorType={}, errorMessage={}",
                    base.qtPassageId(), exception.getClass().getSimpleName(), exception.getMessage());
            return base;
        }
    }

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

}
