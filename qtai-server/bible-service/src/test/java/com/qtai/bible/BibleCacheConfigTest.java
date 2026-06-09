package com.qtai.bible;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * BibleService의 {@code @Cacheable("bibleBooks")}가 동작하도록 CacheManager가 등록되는지 검증.
 * (이 빈이 없으면 @Cacheable이 조용히 무시되어 모놀리식과 캐싱 동작이 달라짐.)
 */
@SpringBootTest(classes = BibleServiceApplication.class)
class BibleCacheConfigTest {

    @Autowired
    CacheManager cacheManager;

    @Test
    void bibleBooksCacheIsRegistered() {
        assertThat(cacheManager).isNotNull();
        assertThat(cacheManager.getCache("bibleBooks")).isNotNull();
    }
}
