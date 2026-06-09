package com.qtai.note;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * 노트/나눔/신고 서비스(note, sharing, report 제출)의 부팅 진입점.
 *
 * <p>Day2-4-1: 웹 스켈레톤만. 도메인 코드(note·sharing·report)와 cross-domain Mock
 * (bible·member·notification·audit·admin), JournalEvent 처리, JPA/보안 설정은 후속 단계에서 이전.
 * <ul>
 *   <li>component scan: {@code com.qtai} — lib-common 공통 빈 + 도메인 컴포넌트</li>
 *   <li>entity/repository scan: {@code com.qtai.domain}</li>
 * </ul>
 */
@SpringBootApplication(scanBasePackages = "com.qtai")
@EntityScan(basePackages = "com.qtai.domain")
@EnableJpaRepositories(basePackages = "com.qtai.domain")
@EnableCaching
public class NoteServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(NoteServiceApplication.class, args);
    }
}
