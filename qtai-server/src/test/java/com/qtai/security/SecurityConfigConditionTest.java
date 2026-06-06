package com.qtai.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;

/**
 * SecurityConfig 활성화 조건 검증 (P1-7 프로파일 갭 수정 회귀 방지).
 *
 * <p>풀 부팅 없이 어노테이션 메타데이터로 다음을 보호한다:
 * <ul>
 *   <li>{@code @Profile("!dev")}가 제거되어 dev+bypass=false 조합에서도 정식 체인이 켜지는지</li>
 *   <li>활성 조건이 dev-bypass=false(또는 미설정)로 묶여 DevSecurityConfig와 상호 배타인지</li>
 * </ul>
 */
class SecurityConfigConditionTest {

    @Test
    @DisplayName("@Profile(\"!dev\")는 제거되어야 한다 — dev+bypass=false 갭(시큐리티 체인 0개) 방지")
    void noProfileRestriction() {
        Profile profile = SecurityConfig.class.getAnnotation(Profile.class);
        assertThat(profile)
                .as("@Profile(\"!dev\")가 남아 있으면 dev+bypass=false에서 정식 체인이 비활성화된다")
                .isNull();
    }

    @Test
    @DisplayName("@ConditionalOnProperty(dev-bypass=false, matchIfMissing=true) — DevSecurityConfig와 배타")
    void activatesWhenBypassDisabledOrMissing() {
        ConditionalOnProperty cond = SecurityConfig.class.getAnnotation(ConditionalOnProperty.class);
        assertThat(cond).isNotNull();
        assertThat(cond.name()).containsExactly("qtai.security.dev-bypass");
        assertThat(cond.havingValue()).isEqualTo("false");
        assertThat(cond.matchIfMissing())
                .as("prod/local처럼 토글 미설정이면 정식 체인이 켜져야 한다")
                .isTrue();
    }
}
