package com.qtai.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;

/**
 * DevSecurityConfig 활성화 토글 검증.
 *
 * SpringBootTest로 ApplicationContext 부팅을 거치면 application-dev.yml의 MySQL 의존성과
 * Flyway가 함께 로드돼 비용이 크다. 대신 클래스에 적용된 @Profile, @ConditionalOnProperty
 * 어노테이션 메타데이터를 reflection으로 검증해 다음을 보호한다:
 *
 *   - prod 환경에서 빈이 절대 생성되지 않도록 @Profile("dev")가 그대로 유지되는지
 *   - 명시적 토글 (qtai.security.dev-bypass=true) 없이는 활성화되지 않는지
 *   - 토글 기본값(havingValue)이 "true"로 묶여 있는지
 *
 * 실 통합 부팅 검증은 이승욱 정식 SecurityConfig 머지 후 SpringBootTest로 추가 예정.
 */
class DevSecurityConfigToggleTest {

    @Test
    @DisplayName("@Profile(\"dev\")만 적용되어 있어 prod/default 프로파일에서는 빈 미등록")
    void hasOnlyDevProfile() {
        Profile profile = DevSecurityConfig.class.getAnnotation(Profile.class);

        assertThat(profile)
                .as("@Profile 어노테이션이 빠지면 prod에서도 활성화 위험")
                .isNotNull();
        assertThat(profile.value())
                .as("@Profile은 'dev' 하나만 지정해야 함 (prod 포함 절대 금지)")
                .containsExactly("dev");
    }

    @Test
    @DisplayName("@ConditionalOnProperty(qtai.security.dev-bypass=true) — 명시적 토글 없으면 비활성")
    void hasExplicitToggleAnnotation() {
        ConditionalOnProperty cond = DevSecurityConfig.class.getAnnotation(ConditionalOnProperty.class);

        assertThat(cond)
                .as("@ConditionalOnProperty 어노테이션이 빠지면 dev 프로파일만으로 자동 활성화 위험")
                .isNotNull();
        assertThat(cond.name())
                .as("토글 키는 qtai.security.dev-bypass 고정")
                .containsExactly("qtai.security.dev-bypass");
        assertThat(cond.havingValue())
                .as("havingValue는 'true' 고정 — 명시적으로 켜야만 활성")
                .isEqualTo("true");
    }
}
