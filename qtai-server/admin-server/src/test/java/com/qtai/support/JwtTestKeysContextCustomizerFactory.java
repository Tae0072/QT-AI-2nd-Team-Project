package com.qtai.support;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.MapPropertySource;
import org.springframework.test.context.ContextConfigurationAttributes;
import org.springframework.test.context.ContextCustomizer;
import org.springframework.test.context.ContextCustomizerFactory;
import org.springframework.test.context.MergedContextConfiguration;

/**
 * 모든 Spring 테스트 컨텍스트에 런타임 생성 RSA 키쌍을 주입한다 (P1-5).
 *
 * <p>기존에는 동일 RSA 개인키가 {@code src/test/resources/application*.yml}에 평문으로
 * 커밋돼 있었고, .env.example이 "시연/로컬은 테스트 키 그대로" 안내해 운영 키로 승격될
 * 위험이 있었다(CLAUDE.md §8). 평문 키를 저장소에서 제거하고, 테스트 부팅 시점에
 * 매번 새 키를 생성해 주입한다.
 *
 * <p>키는 클래스 로드 시 한 번 생성하고 모든 컨텍스트가 공유한다 —
 * {@link ContextCustomizer#equals}/{@code hashCode}를 상수로 두어 Spring 테스트
 * 컨텍스트 캐시가 깨지지 않게 한다.
 *
 * <p>등록: {@code src/test/resources/META-INF/spring.factories}의
 * {@code org.springframework.test.context.ContextCustomizerFactory}.
 */
public class JwtTestKeysContextCustomizerFactory implements ContextCustomizerFactory {

    @Override
    public ContextCustomizer createContextCustomizer(
            Class<?> testClass, List<ContextConfigurationAttributes> configAttributes) {
        return JwtTestKeysContextCustomizer.INSTANCE;
    }

    private static final class JwtTestKeysContextCustomizer implements ContextCustomizer {

        private static final JwtTestKeysContextCustomizer INSTANCE = new JwtTestKeysContextCustomizer();

        private static final String PRIVATE_KEY_BASE64;
        private static final String PUBLIC_KEY_BASE64;

        static {
            try {
                KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
                generator.initialize(2048);
                KeyPair keyPair = generator.generateKeyPair();
                Base64.Encoder encoder = Base64.getEncoder();
                PRIVATE_KEY_BASE64 = encoder.encodeToString(keyPair.getPrivate().getEncoded());
                PUBLIC_KEY_BASE64 = encoder.encodeToString(keyPair.getPublic().getEncoded());
            } catch (Exception e) {
                throw new IllegalStateException("테스트용 RSA 키쌍 생성 실패", e);
            }
        }

        @Override
        public void customizeContext(ConfigurableApplicationContext context,
                                     MergedContextConfiguration mergedConfig) {
            // addFirst로 application*.yml보다 높은 우선순위로 주입(JwtProvider @Value가 읽음)
            context.getEnvironment().getPropertySources().addFirst(new MapPropertySource(
                    "jwtTestKeys",
                    Map.of(
                            "security.jwt.private-key", PRIVATE_KEY_BASE64,
                            "security.jwt.public-key", PUBLIC_KEY_BASE64
                    )
            ));
        }

        // 모든 컨텍스트가 동일 customizer로 취급되도록 상수 equals/hashCode — 테스트 컨텍스트 캐시 보존
        @Override
        public boolean equals(Object obj) {
            return obj instanceof JwtTestKeysContextCustomizer;
        }

        @Override
        public int hashCode() {
            return JwtTestKeysContextCustomizer.class.hashCode();
        }
    }
}
