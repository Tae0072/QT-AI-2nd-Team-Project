package com.qtai.user;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * 사용자/인증 서비스(member, notification, mission)의 부팅 진입점. JWT 발급 주체.
 *
 * <p>Day2-1: 웹 스켈레톤만. 도메인 코드·JWT 발급(JwtProvider)·Kakao OAuth는 Day2-2에서 이전.
 * <ul>
 *   <li>component scan: {@code com.qtai} — lib-common 공통 빈 + 도메인 컴포넌트</li>
 *   <li>entity/repository scan: {@code com.qtai.domain}</li>
 * </ul>
 */
@SpringBootApplication(scanBasePackages = "com.qtai")
@EntityScan(basePackages = "com.qtai.domain")
@EnableJpaRepositories(basePackages = "com.qtai.domain")
@EnableCaching
public class UserServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(UserServiceApplication.class, args);
    }
}
