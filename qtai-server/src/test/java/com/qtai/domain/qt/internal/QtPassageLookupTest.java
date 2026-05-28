package com.qtai.domain.qt.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.qtai.domain.qt.api.dto.TodayQtResponse;

/**
 * QtPassageLookup 단위 테스트 — 캐시 대상 본문 조회 로직.
 *
 * <p>CLAUDE.md §6 정책 (00:00/04:00 KST 캐시 전환)을 검증한다.
 * <p>CLAUDE.md §10 필수 테스트: 00:00/04:00 Today QT cache 동작.
 *
 * <p>이 테스트는 캐시 프록시 없이 순수 로직만 검증한다.
 * 캐시 동작 검증은 {@link QtPassageLookupCacheTest}에서 수행한다.
 */
class QtPassageLookupTest {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

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

    @Nested
    @DisplayName("findTodayPassage — 오늘의 QT 본문 조회")
    class FindTodayPassageTest {

        @Test
        @DisplayName("오늘 본문이 있으면 HIT 반환")
        void 오늘_본문_존재_HIT() {
            // given: 2026-05-28 오후 2시 (배치 이후)
            Clock clock = fixedClockKst(2026, 5, 28, 14, 0);
            QtPassageRepository repo = Mockito.mock(QtPassageRepository.class);
            QtPassageLookup lookup = new QtPassageLookup(repo, clock);

            LocalDate today = LocalDate.of(2026, 5, 28);
            QtPassage passage = createPassage(1L, today, "하나님이 세상을 이처럼 사랑하사");
            when(repo.findByQtDate(today)).thenReturn(Optional.of(passage));

            // when
            TodayQtResponse response = lookup.findTodayPassage();

            // then
            assertThat(response.qtPassageId()).isEqualTo(1L);
            assertThat(response.passageDate()).isEqualTo("2026-05-28");
            assertThat(response.title()).isEqualTo("하나님이 세상을 이처럼 사랑하사");
            assertThat(response.cacheStatus()).isEqualTo("HIT");
            assertThat(response.simulatorStatus()).isEqualTo("MISSING");
            assertThat(response.hasExplanation()).isFalse();
            assertThat(response.draftNoteId()).isNull(); // 공용 캐시는 항상 null
        }

        @Test
        @DisplayName("00:00~04:00 사이 오늘 본문 없으면 어제 본문을 STALE_FALLBACK으로 반환")
        void 새벽_본문_없음_STALE_FALLBACK() {
            // given: 2026-05-28 새벽 2시 (배치 이전)
            Clock clock = fixedClockKst(2026, 5, 28, 2, 0);
            QtPassageRepository repo = Mockito.mock(QtPassageRepository.class);
            QtPassageLookup lookup = new QtPassageLookup(repo, clock);

            LocalDate today = LocalDate.of(2026, 5, 28);
            LocalDate yesterday = LocalDate.of(2026, 5, 27);

            when(repo.findByQtDate(today)).thenReturn(Optional.empty());
            QtPassage yesterdayPassage = createPassage(2L, yesterday, "여호와는 나의 목자시니");
            when(repo.findByQtDate(yesterday)).thenReturn(Optional.of(yesterdayPassage));

            // when
            TodayQtResponse response = lookup.findTodayPassage();

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
            QtPassageRepository repo = Mockito.mock(QtPassageRepository.class);
            QtPassageLookup lookup = new QtPassageLookup(repo, clock);

            LocalDate today = LocalDate.of(2026, 5, 28);
            LocalDate yesterday = LocalDate.of(2026, 5, 27);

            when(repo.findByQtDate(today)).thenReturn(Optional.empty());
            when(repo.findByQtDate(yesterday)).thenReturn(Optional.empty());

            // when
            TodayQtResponse response = lookup.findTodayPassage();

            // then
            assertThat(response.qtPassageId()).isNull();
            assertThat(response.cacheStatus()).isEqualTo("EMPTY");
            assertThat(response.simulatorStatus()).isEqualTo("DISABLED");
        }

        @Test
        @DisplayName("04:00 이후 오늘 본문 없으면 MISS 반환 (배치 미완료)")
        void 배치_이후_본문_없음_MISS() {
            // given: 2026-05-28 오후 5시
            Clock clock = fixedClockKst(2026, 5, 28, 17, 0);
            QtPassageRepository repo = Mockito.mock(QtPassageRepository.class);
            QtPassageLookup lookup = new QtPassageLookup(repo, clock);

            LocalDate today = LocalDate.of(2026, 5, 28);
            when(repo.findByQtDate(today)).thenReturn(Optional.empty());

            // when
            TodayQtResponse response = lookup.findTodayPassage();

            // then
            assertThat(response.qtPassageId()).isNull();
            assertThat(response.cacheStatus()).isEqualTo("MISS");
            assertThat(response.simulatorStatus()).isEqualTo("DISABLED");
        }

        @Test
        @DisplayName("00:00~04:00 사이라도 오늘 본문이 있으면 HIT 반환")
        void 새벽_오늘_본문_있으면_HIT() {
            // given: 2026-05-28 새벽 3시, 이미 오늘 데이터가 준비됨
            Clock clock = fixedClockKst(2026, 5, 28, 3, 0);
            QtPassageRepository repo = Mockito.mock(QtPassageRepository.class);
            QtPassageLookup lookup = new QtPassageLookup(repo, clock);

            LocalDate today = LocalDate.of(2026, 5, 28);
            QtPassage passage = createPassage(3L, today, "태초에 하나님이");
            when(repo.findByQtDate(today)).thenReturn(Optional.of(passage));

            // when
            TodayQtResponse response = lookup.findTodayPassage();

            // then
            assertThat(response.qtPassageId()).isEqualTo(3L);
            assertThat(response.cacheStatus()).isEqualTo("HIT");
        }
    }
}
