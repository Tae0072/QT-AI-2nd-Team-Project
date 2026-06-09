package com.qtai.bible;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * 읽기전용 콘텐츠 서비스(bible, qt, study, music, praise)의 부팅 진입점.
 *
 * <p>③단계: bible 도메인을 파일럿으로 이전했다.
 * <ul>
 *   <li>component scan: {@code com.qtai} — lib-common 공통 빈 + 도메인 컴포넌트</li>
 *   <li>entity/repository scan: {@code com.qtai.domain} — 도메인 엔티티/리포지토리</li>
 *   <li>캐시 활성화: bible_books 등 불변 데이터 Caffeine 캐시</li>
 * </ul>
 */
@SpringBootApplication(scanBasePackages = "com.qtai")
@EntityScan(basePackages = "com.qtai.domain")
@EnableJpaRepositories(basePackages = "com.qtai.domain")
@EnableCaching
public class BibleServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(BibleServiceApplication.class, args);
    }
}
