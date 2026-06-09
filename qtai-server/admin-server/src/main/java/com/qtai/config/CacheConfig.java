package com.qtai.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * Caffeine 캐시 설정.
 *
 * <p>등록된 캐시:
 * <ul>
 *   <li>{@code bibleBooks} — 성경 책 목록 (24시간 TTL)</li>
 *   <li>{@code todayQt} — 오늘의 QT 본문 (1시간 TTL, 00:00/04:00 전환에 대응)</li>
 * </ul>
 */
@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        // bibleBooks: 기본 설정 (24시간 TTL)
        CaffeineCacheManager manager = new CaffeineCacheManager("bibleBooks");
        manager.setCaffeine(Caffeine.newBuilder()
                .maximumSize(100)
                .expireAfterWrite(24, TimeUnit.HOURS));

        // todayQt: 1시간 TTL로 00:00/04:00 전환에 대응
        manager.registerCustomCache("todayQt",
                Caffeine.newBuilder()
                        .maximumSize(10)
                        .expireAfterWrite(1, TimeUnit.HOURS)
                        .build());

        return manager;
    }
}
