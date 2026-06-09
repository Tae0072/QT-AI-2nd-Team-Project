package com.qtai.bible;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 읽기전용 콘텐츠 서비스(bible, qt, study, music, praise)의 부팅 진입점.
 *
 * <p>①단계에서는 웹 스켈레톤만 띄운다. 도메인 코드와 JPA/DB 설정은 ③단계에서 이전한다.
 */
@SpringBootApplication
public class BibleServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(BibleServiceApplication.class, args);
    }
}
