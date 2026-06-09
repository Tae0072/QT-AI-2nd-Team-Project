package com.qtai.note;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 노트/나눔/신고 서비스(note, sharing, report 제출)의 부팅 진입점.
 *
 * <p>Day2-4-2~4: note·sharing·report(제출) 도메인을 이전했다(Strangler — 모놀리식 원본 유지).
 * 타 서비스 의존(bible·member·notification)은 api 계약 타입만 가져와 client 어댑터로 임시 구현하며,
 * 통합 시 RestClient로 교체한다. 신고 검수(admin)는 admin-server 소관이라 포함하지 않는다.
 * <ul>
 *   <li>component scan: {@code com.qtai} — lib-common 공통 빈 + 도메인 컴포넌트</li>
 *   <li>entity/repository scan: {@code com.qtai.domain}</li>
 *   <li>스케줄링 활성화: note의 journal_events 아웃박스 재처리기({@code @Scheduled} 폴링)</li>
 * </ul>
 */
@SpringBootApplication(scanBasePackages = "com.qtai")
@EntityScan(basePackages = "com.qtai.domain")
@EnableJpaRepositories(basePackages = "com.qtai.domain")
@EnableCaching
@EnableScheduling
public class NoteServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(NoteServiceApplication.class, args);
    }
}
