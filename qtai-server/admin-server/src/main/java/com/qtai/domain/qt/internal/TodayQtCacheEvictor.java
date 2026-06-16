package com.qtai.domain.qt.internal;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Component
class TodayQtCacheEvictor {

    static final String CACHE_NAME = "todayQt";

    private final CacheManager cacheManager;

    TodayQtCacheEvictor(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    void evictAfterCommit() {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            clear();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                clear();
            }
        });
    }

    private void clear() {
        Cache cache = cacheManager.getCache(CACHE_NAME);
        if (cache != null) {
            cache.clear();
        }
    }
}
