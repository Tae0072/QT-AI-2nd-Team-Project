package com.qtai.domain.qt.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.qtai.common.exception.BusinessException;
import com.qtai.common.exception.ErrorCode;
import com.qtai.domain.qt.api.dto.TodayQtResponse;

/**
 * QtService 단위 테스트 — GetTodayQtUseCase.
 *
 * <p>CLAUDE.md §6 정책 (00:00/04:00 KST 캐시 전환)을 검증한다.
 * <p>CLAUDE.md §10 필수 테스트: 00:00/04:00 Today QT cache 동작.
 */
class QtServiceTest {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private QtPassageRepository qtPassageRepository;
    private QtService qtService;

    // ------------------------------------------------------------------
    // 테스트용 시간 헬퍼
    // ------------------------------------------------------------------

    /** KST 기준 특정 시각의 고정 Clock 생성. */
    private static Clock fixedClockKst(int year, int month, int day, int hour, int minute) {
        String iso = String.format("%04d-%02d-%02dT%02d:%02d:00+09:00",
                year, month, day, hour, minute);
        Instant instant = Instant.from(java.time.OffsetDateTime.parse(iso));
        return Clock.fixed(instant, KST);
    }

    /** QtPassageFixture 위임. */
    private static QtPassage createPassage(Long id, LocalDate date, String title) {
        return QtPassageFixture.createPassage(id, date, title);
    }

    // ------------------------------------------------------------------
    // GetTodayQtUseCase.getToday() 테스트
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("getToday — 오늘의 QT 조회")
    class GetTodayTest {

        @Test
        @DisplayName("오늘 본문이 있으면 HIT 반환")
        void 오늘_본문_존재_HIT() {
            // given: 2026-05-28 오후 2시 (배치 이후)
            Clock clock = fixedClockKst(2026, 5, 28, 14, 0);
            qtPassageRepository = Mockito.mock(QtPassageRepository.class);
            qtService = new QtService(qtPassageRepository, clock);

            LocalDate today = LocalDate.of(2026, 5, 28);
            QtPassage passage = createPassage(1L, today, "하나님이 세상을 이처럼 사랑하사");
            when(qtPassageRepository.findByQtDate(today)).thenReturn(Optional.of(passage));

            // when
            TodayQtResponse response = qtService.getToday(100L);

            // then
            assertThat(response.qtPassageId()).isEqualTo(1L);
            assertThat(response.passageDate()).isEqualTo("2026-05-28");
            assertThat(response.title()).isEqualTo("하나님이 세상을 이처럼 사랑하사");
            assertThat(response.cacheStatus()).isEqualTo("HIT");
            assertThat(response.simulatorStatus()).isEqualTo("MISSING"); // 1차: 기본값
            assertThat(response.hasExplanation()).isFalse();             // 1차: 기본값
            assertThat(response.draftNoteId()).isNull();                 // 1차: 기본값
        }

        @Test
        @DisplayName("00:00~04:00 사이 오늘 본문 없으면 어제 본문을 STALE_FALLBACK으로 반환")
        void 새벽_본문_없음_STALE_FALLBACK() {
            // given: 2026-05-28 새벽 2시 (배치 이전)
            Clock clock = fixedClockKst(2026, 5, 28, 2, 0);
            qtPassageRepository = Mockito.mock(QtPassageRepository.class);
            qtService = new QtService(qtPassageRepository, clock);

            LocalDate today = LocalDate.of(2026, 5, 28);
            LocalDate yesterday = LocalDate.of(2026, 5, 27);

            when(qtPassageRepository.findByQtDate(today)).thenReturn(Optional.empty());
            QtPassage yesterdayPassage = createPassage(2L, yesterday, "여호와는 나의 목자시니");
            when(qtPassageRepository.findByQtDate(yesterday)).thenReturn(Optional.of(yesterdayPassage));

            // when
            TodayQtResponse response = qtService.getToday(100L);

            // then
            assertThat(response.qtPassageId()).isEqualTo(2L);
            assertThat(response.passageDate()).isEqualTo("2026-05-27");
            assertThat(response.cacheStatus()).isEqualTo("STALE_FALLBACK");
        }

        @Test
        @DisplayName("00:00~04:00 사이 어제 본문도 없으면 EMPTY 반환")
        void 새벽_어제도_없음_EMPTY() {
            // given: 2026-05-28 새벽 1시, 어제 데이터도 없음
            Clock clock = fixedClockKst(2026, 5, 28, 1, 0);
            qtPassageRepository = Mockito.mock(QtPassageRepository.class);
            qtService = new QtService(qtPassageRepository, clock);

            LocalDate today = LocalDate.of(2026, 5, 28);
            LocalDate yesterday = LocalDate.of(2026, 5, 27);

            when(qtPassageRepository.findByQtDate(today)).thenReturn(Optional.empty());
            when(qtPassageRepository.findByQtDate(yesterday)).thenReturn(Optional.empty());

            // when
            TodayQtResponse response = qtService.getToday(100L);

            // then
            assertThat(response.qtPassageId()).isNull();
            assertThat(response.cacheStatus()).isEqualTo("EMPTY");
            assertThat(response.simulatorStatus()).isEqualTo("DISABLED"); // 데이터 없음 → DISABLED
        }

        @Test
        @DisplayName("04:00 이후 오늘 본문 없으면 MISS 반환 (배치 미완료)")
        void 배치_이후_본문_없음_MISS() {
            // given: 2026-05-28 오후 5시 (배치 이후인데 데이터 없음)
            Clock clock = fixedClockKst(2026, 5, 28, 17, 0);
            qtPassageRepository = Mockito.mock(QtPassageRepository.class);
            qtService = new QtService(qtPassageRepository, clock);

            LocalDate today = LocalDate.of(2026, 5, 28);
            when(qtPassageRepository.findByQtDate(today)).thenReturn(Optional.empty());

            // when
            TodayQtResponse response = qtService.getToday(100L);

            // then
            assertThat(response.qtPassageId()).isNull();
            assertThat(response.cacheStatus()).isEqualTo("MISS");
            assertThat(response.simulatorStatus()).isEqualTo("DISABLED"); // 데이터 없음 → DISABLED
        }

        @Test
        @DisplayName("00:00~04:00 사이라도 오늘 본문이 있으면 HIT 반환")
        void 새벽_오늘_본문_있으면_HIT() {
            // given: 2026-05-28 새벽 3시, 이미 오늘 데이터가 준비됨
            Clock clock = fixedClockKst(2026, 5, 28, 3, 0);
            qtPassageRepository = Mockito.mock(QtPassageRepository.class);
            qtService = new QtService(qtPassageRepository, clock);

            LocalDate today = LocalDate.of(2026, 5, 28);
            QtPassage passage = createPassage(3L, today, "태초에 하나님이");
            when(qtPassageRepository.findByQtDate(today)).thenReturn(Optional.of(passage));

            // when
            TodayQtResponse response = qtService.getToday(100L);

            // then
            assertThat(response.qtPassageId()).isEqualTo(3L);
            assertThat(response.cacheStatus()).isEqualTo("HIT");
        }

        @Test
        @DisplayName("principal 미해석 시에도 방어적으로 정상 동작")
        void principal_미해석_시_방어적_처리() {
            // given: memberId null (@AuthenticationPrincipal 해석 실패 시 방어적 처리)
            Clock clock = fixedClockKst(2026, 5, 28, 14, 0);
            qtPassageRepository = Mockito.mock(QtPassageRepository.class);
            qtService = new QtService(qtPassageRepository, clock);

            LocalDate today = LocalDate.of(2026, 5, 28);
            QtPassage passage = createPassage(1L, today, "테스트 본문");
            when(qtPassageRepository.findByQtDate(today)).thenReturn(Optional.of(passage));

            // when
            TodayQtResponse response = qtService.getToday(null);

            // then
            assertThat(response.qtPassageId()).isEqualTo(1L);
            assertThat(response.cacheStatus()).isEqualTo("HIT");
        }
    }

    // ------------------------------------------------------------------
    // GetTodayQtUseCase.getPassage() 테스트
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("getPassage — 특정 QT 본문 조회")
    class GetPassageTest {

        @BeforeEach
        void setUp() {
            Clock clock = fixedClockKst(2026, 5, 28, 14, 0);
            qtPassageRepository = Mockito.mock(QtPassageRepository.class);
            qtService = new QtService(qtPassageRepository, clock);
        }

        @Test
        @DisplayName("존재하는 본문 ID로 조회하면 HIT 반환")
        void 존재하는_본문_조회_성공() {
            // given
            QtPassage passage = createPassage(5L, LocalDate.of(2026, 5, 26), "태초에 하나님이");
            when(qtPassageRepository.findById(5L)).thenReturn(Optional.of(passage));

            // when
            TodayQtResponse response = qtService.getPassage(100L, 5L);

            // then
            assertThat(response.qtPassageId()).isEqualTo(5L);
            assertThat(response.passageDate()).isEqualTo("2026-05-26");
            assertThat(response.title()).isEqualTo("태초에 하나님이");
            assertThat(response.cacheStatus()).isEqualTo("HIT");
        }

        @Test
        @DisplayName("존재하지 않는 본문 ID로 조회하면 QT_PASSAGE_NOT_FOUND")
        void 존재하지_않는_본문_조회_실패() {
            // given
            when(qtPassageRepository.findById(999L)).thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> qtService.getPassage(100L, 999L))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> {
                        BusinessException be = (BusinessException) ex;
                        assertThat(be.getErrorCode()).isEqualTo(ErrorCode.QT_PASSAGE_NOT_FOUND);
                    });
        }
    }
}
