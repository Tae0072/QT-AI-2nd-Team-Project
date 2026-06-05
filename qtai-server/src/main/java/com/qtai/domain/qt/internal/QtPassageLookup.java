package com.qtai.domain.qt.internal;

import com.qtai.domain.qt.api.dto.TodayQtResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

/**
 * QT 본문 캐시 조회 전용 컴포넌트.
 *
 * <p>QtService에서 분리한 이유:
 * {@code @Cacheable}은 Spring 프록시를 통해야 동작하므로,
 * 캐시된 공용 데이터(본문/해설/시뮬레이터)와 사용자별 데이터(draftNoteId)를
 * 분리하려면 별도 빈이 필요하다.
 *
 * <p>이 컴포넌트가 반환하는 응답에는 사용자별 데이터가 없다(draftNoteId=null).
 * 사용자별 데이터는 QtService에서 캐시 바깥에서 enrich한다.
 *
 * @see QtService#getToday(Long)
 */
@Slf4j
@Component
@RequiredArgsConstructor
class QtPassageLookup {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final LocalTime BATCH_COMPLETE_TIME = LocalTime.of(4, 0);

    private final QtPassageRepository qtPassageRepository;
    private final Clock clock;
    private final TodayQtRangeResolver rangeResolver;

    /**
     * 오늘의 QT 본문을 캐시에서 조회한다.
     *
     * <p>캐시 정책 (CLAUDE.md §6):
     * <ul>
     *   <li>00:00~04:00 사이에는 오늘 본문이 DB에 있어도 어제 본문을 {@code STALE_FALLBACK}으로 반환</li>
     *   <li>04:00 이후 오늘 날짜의 QT 본문이 있으면 {@code HIT}으로 반환</li>
     *   <li>04:00 이후 오늘 본문 없으면 {@code MISS}로 반환 (클라이언트 재시도 권장)</li>
     *   <li>어떤 데이터도 없으면 {@code EMPTY}</li>
     * </ul>
     *
     * <p>반환 응답의 {@code draftNoteId}는 항상 null이다.
     * 사용자별 데이터는 QtService에서 enrich한다.
     *
     * @return 공용 캐시 응답 (draftNoteId=null)
     */
    @Cacheable(cacheNames = "todayQt",
            key = "T(java.time.LocalDate).now(T(java.time.ZoneId).of('Asia/Seoul')).toString()",
            unless = "!#result.cacheStatus().equals('HIT')")
    public TodayQtResponse findTodayPassage() {
        ZonedDateTime nowKst = ZonedDateTime.now(clock).withZoneSameInstant(KST);
        LocalDate today = nowKst.toLocalDate();
        boolean isBeforeBatch = nowKst.toLocalTime().isBefore(BATCH_COMPLETE_TIME);

        if (isBeforeBatch) {
            return qtPassageRepository.findByQtDate(today.minusDays(1))
                    .map(passage -> toResponse(passage, "STALE_FALLBACK"))
                    .orElse(emptyResponse());
        }

        return qtPassageRepository.findByQtDate(today)
                .map(passage -> toResponse(passage, "HIT"))
                .orElseGet(() -> {
                    log.warn("오늘의 QT 본문이 없습니다. date={}, 배치 상태를 확인해 주세요.", today);
                    return emptyResponse("MISS");
                });
    }

    private TodayQtResponse toResponse(QtPassage passage, String cacheStatus) {
        return new TodayQtResponse(
                passage.getId(),
                passage.getQtDate().toString(),
                passage.getTitle(),
                "MISSING",    // simulatorStatus 기본값 — QtService가 캐시 밖에서 study 가용성으로 enrich
                false,        // hasExplanation 기본값 — QtService가 캐시 밖에서 study 가용성으로 enrich
                null,         // draftNoteId: QtService에서 enrich
                cacheStatus,
                rangeResolver.resolve(passage)
        );
    }

    private TodayQtResponse emptyResponse() {
        return emptyResponse("EMPTY");
    }

    private TodayQtResponse emptyResponse(String cacheStatus) {
        // DISABLED는 '운영자 비활성' 의미 — 본문 부재는 콘텐츠 없음(MISSING)으로 표현한다
        return new TodayQtResponse(null, null, null, "MISSING", false, null, cacheStatus);
    }
}
