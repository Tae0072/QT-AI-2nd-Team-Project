package com.qtai.domain.qt.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qtai.domain.audit.api.WriteAuditLogUseCase;
import com.qtai.domain.qt.api.admin.HideAdminQtPassageUseCase;
import com.qtai.domain.qt.api.admin.PublishAdminQtPassageUseCase;
import com.qtai.domain.qt.api.admin.UpdateAdminQtPassageUseCase;
import com.qtai.domain.qt.api.admin.dto.AdminQtPassageCommand;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

@SpringJUnitConfig
@ContextConfiguration(classes = AdminQtPassageCacheEvictTest.TestConfig.class)
class AdminQtPassageCacheEvictTest {

    private static final Clock CLOCK =
            Clock.fixed(Instant.parse("2026-06-10T01:30:00Z"), ZoneId.of("Asia/Seoul"));

    private final UpdateAdminQtPassageUseCase updateUseCase;
    private final PublishAdminQtPassageUseCase publishUseCase;
    private final HideAdminQtPassageUseCase hideUseCase;
    private final QtPassageRepository repository;
    private final WriteAuditLogUseCase auditLogUseCase;
    private final Cache todayQtCache;

    @Autowired
    AdminQtPassageCacheEvictTest(
            UpdateAdminQtPassageUseCase updateUseCase,
            PublishAdminQtPassageUseCase publishUseCase,
            HideAdminQtPassageUseCase hideUseCase,
            QtPassageRepository repository,
            WriteAuditLogUseCase auditLogUseCase,
            CacheManager cacheManager
    ) {
        this.updateUseCase = updateUseCase;
        this.publishUseCase = publishUseCase;
        this.hideUseCase = hideUseCase;
        this.repository = repository;
        this.auditLogUseCase = auditLogUseCase;
        this.todayQtCache = cacheManager.getCache("todayQt");
    }

    @BeforeEach
    void setUp() {
        reset(repository, auditLogUseCase);
        todayQtCache.clear();
        todayQtCache.put("dashboard", "stale");
    }

    @Test
    void updateEvictsTodayQtCacheThroughSpringProxy() {
        AdminQtPassageCommand command = command(LocalDate.of(2026, 6, 11));
        when(repository.findById(20L)).thenReturn(Optional.of(passage(20L, LocalDate.of(2026, 6, 10))));
        when(repository.existsByQtDateAndIdNot(command.qtDate(), 20L)).thenReturn(false);

        updateUseCase.update(20L, command);

        assertThat(todayQtCache.get("dashboard")).isNull();
    }

    @Test
    void publishEvictsTodayQtCacheThroughSpringProxy() {
        when(repository.findById(20L)).thenReturn(Optional.of(passage(20L, LocalDate.of(2026, 6, 10))));

        publishUseCase.publish(3L, 20L);

        assertThat(todayQtCache.get("dashboard")).isNull();
    }

    @Test
    void hideEvictsTodayQtCacheThroughSpringProxy() {
        QtPassage passage = passage(20L, LocalDate.of(2026, 6, 10));
        passage.publish(LocalDate.of(2026, 6, 10).atStartOfDay());
        when(repository.findById(20L)).thenReturn(Optional.of(passage));

        hideUseCase.hide(3L, 20L);

        assertThat(todayQtCache.get("dashboard")).isNull();
    }

    private static AdminQtPassageCommand command(LocalDate qtDate) {
        return new AdminQtPassageCommand(
                3L,
                qtDate,
                (short) 19,
                (short) 23,
                (short) 23,
                (short) 1,
                (short) 6,
                "Admin QT",
                "Ps 23:1-6"
        );
    }

    private static QtPassage passage(Long id, LocalDate qtDate) {
        QtPassage passage = QtPassage.create(
                qtDate,
                (short) 19,
                (short) 23,
                (short) 1,
                (short) 6,
                "Admin QT",
                "Ps 23:1-6"
        );
        org.springframework.test.util.ReflectionTestUtils.setField(passage, "id", id);
        return passage;
    }

    @Configuration
    @EnableCaching
    static class TestConfig {

        @Bean
        CacheManager cacheManager() {
            return new ConcurrentMapCacheManager("todayQt");
        }

        @Bean
        QtPassageRepository qtPassageRepository() {
            return mock(QtPassageRepository.class);
        }

        @Bean
        WriteAuditLogUseCase writeAuditLogUseCase() {
            WriteAuditLogUseCase mock = mock(WriteAuditLogUseCase.class);
            org.mockito.Mockito.lenient().doNothing().when(mock).write(any());
            return mock;
        }

        @Bean
        TodayQtCacheEvictor todayQtCacheEvictor(CacheManager cacheManager) {
            return new TodayQtCacheEvictor(cacheManager);
        }

        @Bean
        AdminQtPassageService adminQtPassageService(
                QtPassageRepository repository,
                WriteAuditLogUseCase auditLogUseCase,
                TodayQtCacheEvictor todayQtCacheEvictor
        ) {
            return new AdminQtPassageService(
                    repository,
                    auditLogUseCase,
                    new ObjectMapper().findAndRegisterModules(),
                    CLOCK,
                    todayQtCacheEvictor
            );
        }
    }
}
