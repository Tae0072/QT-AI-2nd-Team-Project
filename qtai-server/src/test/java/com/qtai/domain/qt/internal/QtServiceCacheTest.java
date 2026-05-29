package com.qtai.domain.qt.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.qtai.domain.qt.api.dto.TodayQtResponse;

/**
 * QtPassageLookup 캐시 통합 테스트.
 *
 * <p>Spring 프록시를 통해 @Cacheable 동작을 검증한다:
 * <ul>
 *   <li>HIT 응답은 캐싱되어 repository 재호출 방지</li>
 *   <li>MISS/EMPTY 등 non-HIT 응답은 캐싱하지 않음 (unless 조건)</li>
 * </ul>
 *
 * <p>캐시는 QtPassageLookup에 위치하며, QtService는 캐시 바깥에서
 * 사용자별 데이터(draftNoteId)를 enrich한다.
 *
 * <p>CLAUDE.md §10 필수 테스트: 00:00/04:00 Today QT cache 동작.
 */
@SpringJUnitConfig
@Import(QtServiceCacheTest.CacheTestConfig.class)
class QtServiceCacheTest {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    /** 캐시 프록시를 거치도록 직접 빈 주입. */
    @Autowired
    private QtPassageLookup passageLookup;

    @Autowired
    private QtPassageRepository qtPassageRepository;

    @Autowired
    private CacheManager cacheManager;

    @BeforeEach
    void setUp() {
        // 테스트 간 mock 호출 카운트 및 캐시 초기화
        Mockito.reset(qtPassageRepository);
        cacheManager.getCache("todayQt").clear();
    }

    @TestConfiguration
    @EnableCaching
    static class CacheTestConfig {

        /** 테스트용 KST 오후 2시 고정 Clock. */
        @Bean
        Clock clock() {
            String iso = "2026-05-28T14:00:00+09:00";
            Instant instant = Instant.from(java.time.OffsetDateTime.parse(iso));
            return Clock.fixed(instant, KST);
        }

        @Bean
        QtPassageRepository qtPassageRepository() {
            return org.mockito.Mockito.mock(QtPassageRepository.class);
        }

        @Bean
        QtPassageLookup qtPassageLookup(QtPassageRepository repo, Clock clock) {
            return new QtPassageLookup(repo, clock);
        }

        @Bean
        CacheManager cacheManager() {
            CaffeineCacheManager manager = new CaffeineCacheManager();
            manager.registerCustomCache("todayQt",
                    Caffeine.newBuilder()
                            .maximumSize(10)
                            .build());
            return manager;
        }
    }

    /** QtPassageFixture 위임. */
    private static QtPassage createPassage(Long id, LocalDate date, String title) {
        return QtPassageFixture.createPassage(id, date, title);
    }

    @Test
    @DisplayName("HIT 응답은 캐싱 -> 두 번째 호출 시 repository 미호출")
    void HIT_응답_캐싱_검증() {
        // given
        LocalDate today = LocalDate.of(2026, 5, 28);
        QtPassage passage = createPassage(1L, today, "테스트 본문");
        when(qtPassageRepository.findByQtDate(today)).thenReturn(Optional.of(passage));

        // when: 두 번 호출
        TodayQtResponse first = passageLookup.findTodayPassage();
        TodayQtResponse second = passageLookup.findTodayPassage();

        // then: repository는 1번만 호출 (두 번째는 캐시에서)
        verify(qtPassageRepository, times(1)).findByQtDate(today);
        assertThat(first.cacheStatus()).isEqualTo("HIT");
        assertThat(second.cacheStatus()).isEqualTo("HIT");
        assertThat(first.qtPassageId()).isEqualTo(second.qtPassageId());
    }

    @Test
    @DisplayName("MISS 응답은 캐싱하지 않음 -> 매번 repository 호출")
    void MISS_응답_미캐싱_검증() {
        // given: 04:00 이후인데 데이터 없음 -> MISS
        // CacheTestConfig의 Clock이 14:00 KST이므로 배치 이후 시나리오
        LocalDate today = LocalDate.of(2026, 5, 28);
        when(qtPassageRepository.findByQtDate(today)).thenReturn(Optional.empty());

        // when: 두 번 호출
        TodayQtResponse first = passageLookup.findTodayPassage();
        TodayQtResponse second = passageLookup.findTodayPassage();

        // then: repository는 2번 호출 (캐싱되지 않으므로)
        verify(qtPassageRepository, times(2)).findByQtDate(today);
        assertThat(first.cacheStatus()).isEqualTo("MISS");
        assertThat(second.cacheStatus()).isEqualTo("MISS");
    }
}
