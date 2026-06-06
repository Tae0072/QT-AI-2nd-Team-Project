package com.qtai.domain.qt.internal;

import static org.assertj.core.api.Assertions.assertThat;

import com.qtai.domain.qt.api.GetTodayQtUseCase;
import com.qtai.domain.qt.api.dto.TodayQtResponse;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/**
 * Today QT 조립 플로우 풀 컨텍스트 통합 테스트 (3단계 E2E).
 *
 * <p>기존 qt 테스트는 슬라이스(@WebMvcTest)·단위(mock)뿐이라 실제 빈 와이어링을 관통하지 않는다.
 * 이 테스트는 전체 ApplicationContext를 띄워, {@link GetTodayQtUseCase}가 qt 본문 + note 도메인
 * (실제 {@code GetNoteUseCase} 빈)을 조합하는 흐름을 H2에서 end-to-end로 검증한다.
 *
 * <p>검증 포인트(CLAUDE.md §6/§10):
 * <ul>
 *   <li>고정 Clock으로 "오늘"을 결정 — 04:00 배치 이후 → today 본문 HIT 경로</li>
 *   <li>시뮬레이터 상태는 허용 enum(READY/MISSING/FAILED/DISABLED)만 반환</li>
 *   <li>본문이 없을 때 MISSING + MISS로 안전하게 응답(00:00~04:00 정책의 04:00 이후)</li>
 *   <li>note 도메인 실호출 — 드래프트 없는 회원은 draftNoteId=null (cross-domain 무결성)</li>
 * </ul>
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@Import(TodayQtFlowIntegrationTest.FixedClockConfig.class)
class TodayQtFlowIntegrationTest {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    /** 2026-05-27 — 시드 본문 날짜와 일치시킬 "오늘". */
    private static final LocalDate TODAY = LocalDate.of(2026, 5, 27);
    private static final long MEMBER_WITHOUT_DRAFT = 999_999L;

    /** Clock을 06:00 KST(수집 배치 04:00 이후)로 고정해 today 본문 HIT 경로를 결정적으로 만든다. */
    @TestConfiguration
    static class FixedClockConfig {
        @Bean
        @Primary
        Clock fixedClock() {
            return Clock.fixed(TODAY.atTime(6, 0).atZone(KST).toInstant(), KST);
        }
    }

    @Autowired
    private GetTodayQtUseCase getTodayQtUseCase;
    @Autowired
    private QtPassageRepository qtPassageRepository;
    @Autowired
    private CacheManager cacheManager;

    @BeforeEach
    void clearTodayQtCache() {
        evictTodayQtCache();
    }

    @AfterEach
    void clearTodayQtCacheAfter() {
        evictTodayQtCache();
    }

    private void evictTodayQtCache() {
        var cache = cacheManager.getCache("todayQt");
        if (cache != null) {
            cache.clear();
        }
    }

    @Test
    void 오늘_본문이_있으면_조립해서_반환하고_시뮬레이터상태는_허용값이다() {
        qtPassageRepository.saveAndFlush(
                QtPassageFixture.createPassage(null, TODAY, "여호와는 나의 목자시니"));

        TodayQtResponse res = getTodayQtUseCase.getToday(MEMBER_WITHOUT_DRAFT);

        assertThat(res.qtPassageId()).isNotNull();
        assertThat(res.title()).isEqualTo("여호와는 나의 목자시니");
        assertThat(res.passageDate()).isEqualTo("2026-05-27");
        assertThat(res.cacheStatus()).isEqualTo("HIT");
        // CLAUDE.md §6: 시뮬레이터 상태는 정해진 enum 값만 반환한다.
        assertThat(res.simulatorStatus()).isIn("READY", "MISSING", "FAILED", "DISABLED");
        // note 도메인 실호출 — 드래프트 없는 회원은 null (cross-domain 조립 무결성).
        assertThat(res.draftNoteId()).isNull();
    }

    @Test
    void 오늘_본문이_없으면_MISSING과_MISS로_안전하게_응답한다() {
        // 본문 시드 없음 (04:00 이후 → 어제 fallback 없이 MISS)
        // 본문 부재는 MISSING(콘텐츠 없음) — DISABLED는 운영자 비활성 전용 의미
        TodayQtResponse res = getTodayQtUseCase.getToday(MEMBER_WITHOUT_DRAFT);

        assertThat(res.qtPassageId()).isNull();
        assertThat(res.simulatorStatus()).isEqualTo("MISSING");
        assertThat(res.cacheStatus()).isEqualTo("MISS");
    }
}
