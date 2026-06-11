package com.qtai.bible;

import java.util.concurrent.TimeUnit;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * bible-service Caffeine 캐시 설정.
 *
 * <p>BibleService의 {@code @Cacheable("bibleBooks")}가 동작하도록 CacheManager를 등록한다.
 * 모놀리식 {@code com.qtai.config.CacheConfig}에서 bible 관련 캐시(`bibleBooks`, 24h)를 가져왔다.
 * 이 설정이 없으면 @Cacheable이 조용히 무시되어 모놀리식과 캐싱 동작이 달라진다.
 */
@Configuration
@EnableCaching
public class BibleCacheConfig {

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager manager = new CaffeineCacheManager("bibleBooks");
        manager.setCaffeine(Caffeine.newBuilder()
                .maximumSize(100)
                .expireAfterWrite(24, TimeUnit.HOURS));
        return manager;
    }
}
