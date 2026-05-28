package com.qtai.domain.qt.internal;

import com.qtai.common.exception.BusinessException;
import com.qtai.common.exception.ErrorCode;
import com.qtai.domain.qt.api.GetTodayQtUseCase;
import com.qtai.domain.qt.api.dto.TodayQtResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

/**
 * QT 도메인 서비스 — 1차: GetTodayQtUseCase 구현.
 *
 * <p>오늘의 QT 본문 조회와 특정 본문 단건 조회를 담당한다.
 * 00:00~04:00 KST 구간에는 전일 캐시를 STALE_FALLBACK으로 제공한다.
 *
 * <p>타 도메인 접근은 client/ 어댑터로만:
 * <ul>
 *   <li>member.GetMemberUseCase — 작성자 검증/닉네임</li>
 *   <li>bible.GetBibleVerseUseCase — 참조 절 검증/표시</li>
 *   <li>ai.GenerateAiResponseUseCase — 선택적 AI 피드백</li>
 * </ul>
 *
 * <p>TODO: CreateQtUseCase, GetQtUseCase, ListMyQtUseCase, UpdateQtUseCase, DeleteQtUseCase (후속 PR)
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class QtService implements GetTodayQtUseCase {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    /** 배치 완료 시각. 이 시각 이전에는 전일 캐시를 제공한다. */
    private static final LocalTime BATCH_COMPLETE_TIME = LocalTime.of(4, 0);

    private final QtPassageRepository qtPassageRepository;
    private final Clock clock;

    // TODO: final QtRepository qtRepository;
    // TODO: final GetMemberUseCase getMemberUseCase;
    // TODO: final GetBibleVerseUseCase getBibleVerseUseCase;
    // TODO: final GenerateAiResponseUseCase generateAiResponseUseCase;

    // ------------------------------------------------------------------
    // GetTodayQtUseCase 구현
    // ------------------------------------------------------------------

    /**
     * 오늘의 QT 본문 통합 응답을 반환한다.
     *
     * <p>캐시 정책 (CLAUDE.md §6):
     * <ul>
     *   <li>오늘 날짜의 QT 본문이 있으면 {@code HIT}으로 반환</li>
     *   <li>00:00~04:00 사이 오늘 본문 없으면 어제 본문을 {@code STALE_FALLBACK}으로 반환</li>
     *   <li>04:00 이후 오늘 본문 없으면 {@code MISS}로 반환 (클라이언트 재시도 권장)</li>
     *   <li>어떤 데이터도 없으면 {@code EMPTY}</li>
     * </ul>
     *
     * @param memberId 인증된 사용자 ID (DRAFT 노트 조회용, 노트 도메인 연동 후 활용 예정)
     */
    @Override
    @Cacheable(cacheNames = "todayQt",
            key = "T(java.time.LocalDate).now(T(java.time.ZoneId).of('Asia/Seoul')).toString()",
            unless = "!#result.cacheStatus().equals('HIT')")
    public TodayQtResponse getToday(Long memberId) {
        ZonedDateTime nowKst = ZonedDateTime.now(clock).withZoneSameInstant(KST);
        LocalDate today = nowKst.toLocalDate();
        boolean isBeforeBatch = nowKst.toLocalTime().isBefore(BATCH_COMPLETE_TIME);

        // 1. 오늘 날짜의 QT 본문 조회
        return qtPassageRepository.findByQtDate(today)
                .map(passage -> toResponse(passage, "HIT"))
                .orElseGet(() -> {
                    if (isBeforeBatch) {
                        // 2. 00:00~04:00 구간: 어제 본문을 STALE_FALLBACK으로 제공
                        return qtPassageRepository.findByQtDate(today.minusDays(1))
                                .map(passage -> toResponse(passage, "STALE_FALLBACK"))
                                .orElse(emptyResponse());
                    }
                    // 3. 04:00 이후인데 데이터 없음: MISS (배치 미완료)
                    log.warn("오늘의 QT 본문이 없습니다. date={}, 배치 상태를 확인해 주세요.", today);
                    return emptyResponse("MISS");
                });
    }

    /**
     * 특정 QT 본문을 ID로 조회한다.
     *
     * @param memberId    인증된 사용자 ID (DRAFT 노트 조회용, 노트 도메인 연동 후 활용 예정)
     * @param qtPassageId QT 본문 식별자
     * @return QT 본문 통합 응답
     * @throws BusinessException QT_PASSAGE_NOT_FOUND — 본문이 존재하지 않을 때
     */
    @Override
    public TodayQtResponse getPassage(Long memberId, Long qtPassageId) {
        QtPassage passage = qtPassageRepository.findById(qtPassageId)
                .orElseThrow(() -> new BusinessException(ErrorCode.QT_PASSAGE_NOT_FOUND));
        return toResponse(passage, "HIT");
    }

    // ------------------------------------------------------------------
    // Private helpers
    // ------------------------------------------------------------------

    /**
     * QtPassage 엔티티를 TodayQtResponse DTO로 변환한다.
     *
     * <p>현재 1차 구현에서는 해설, 시뮬레이터, 노트 진입점은 기본값을 사용한다.
     * 각 도메인 연동 시 실제 데이터로 교체 예정.
     */
    private TodayQtResponse toResponse(QtPassage passage, String cacheStatus) {
        return new TodayQtResponse(
                passage.getId(),
                passage.getQtDate().toString(),
                passage.getTitle(),
                "MISSING",    // simulatorStatus: 시뮬레이터 도메인 연동 전 기본값
                false,        // hasExplanation: AI 해설 도메인 연동 전 기본값
                null,         // draftNoteId: 노트 도메인 연동 전 기본값
                cacheStatus
        );
    }

    /** 데이터 없음 응답 (EMPTY). */
    private TodayQtResponse emptyResponse() {
        return emptyResponse("EMPTY");
    }

    /** 데이터 없음 응답 (지정 cacheStatus). */
    private TodayQtResponse emptyResponse(String cacheStatus) {
        return new TodayQtResponse(null, null, null, "DISABLED", false, null, cacheStatus);
    }

    // ------------------------------------------------------------------
    // TODO: 후속 PR에서 구현 예정
    // ------------------------------------------------------------------
    // @Transactional createQt — 작성자/절 검증 → INSERT → QtResponse
    // getQt(viewerId, qtId) — 가시 권한 검사 후 반환
    // listMyQt(memberId, pageable) — 본인 작성만
    // @Transactional updateQt — 본인 검증 → 부분 업데이트 (null=유지)
    // @Transactional deleteQt — 본인 검증 → 삭제
}
