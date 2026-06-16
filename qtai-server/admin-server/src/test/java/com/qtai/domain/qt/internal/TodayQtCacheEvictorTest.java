package com.qtai.domain.qt.internal;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.cache.Cache;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

class TodayQtCacheEvictorTest {

    private final ConcurrentMapCacheManager cacheManager =
            new ConcurrentMapCacheManager(TodayQtCacheEvictor.CACHE_NAME);
    private final TodayQtCacheEvictor evictor = new TodayQtCacheEvictor(cacheManager);

    @AfterEach
    void tearDown() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void evictAfterCommitClearsImmediatelyWhenNoTransactionSynchronization() {
        Cache cache = todayQtCache();
        cache.put("dashboard", "stale");

        evictor.evictAfterCommit();

        assertThat(cache.get("dashboard")).isNull();
    }

    @Test
    void evictAfterCommitWaitsUntilAfterCommitWhenTransactionSynchronizationIsActive() {
        Cache cache = todayQtCache();
        cache.put("dashboard", "stale");
        TransactionSynchronizationManager.initSynchronization();

        evictor.evictAfterCommit();

        assertThat(cache.get("dashboard")).isNotNull();
        TransactionSynchronization synchronization =
                TransactionSynchronizationManager.getSynchronizations().get(0);

        synchronization.afterCommit();

        assertThat(cache.get("dashboard")).isNull();
    }

    private Cache todayQtCache() {
        return cacheManager.getCache(TodayQtCacheEvictor.CACHE_NAME);
    }
}
