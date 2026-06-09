package com.qtai.gateway.support;

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
 * 게이트웨이 테스트 컨텍스트에 런타임 생성 RSA 공개키를 주입한다.
 *
 * <p>모놀리식의 {@code com.qtai.support.JwtTestKeysContextCustomizerFactory}와 동일한 패턴.
 * 평문 키를 저장소에 커밋하지 않고(CLAUDE.md §8 — 테스트 키가 운영 키로 승격될 위험 차단),
 * 테스트 부팅 시점에 키를 생성해 {@code gateway.jwt.public-key}로 주입한다. 게이트웨이는
 * 검증 전용이라 공개키만 주입한다(발급=개인키는 인증 서비스에만).
 *
 * <p>키는 클래스 로드 시 한 번 생성해 모든 컨텍스트가 공유하고,
 * {@link ContextCustomizer#equals}/{@code hashCode}를 상수로 두어 테스트 컨텍스트 캐시를 보존한다.
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

        private static final String PUBLIC_KEY_BASE64;

        static {
            try {
                KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
                generator.initialize(2048);
                KeyPair keyPair = generator.generateKeyPair();
                PUBLIC_KEY_BASE64 = Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded());
            } catch (Exception e) {
                throw new IllegalStateException("테스트용 RSA 키쌍 생성 실패", e);
            }
        }

        @Override
        public void customizeContext(ConfigurableApplicationContext context,
                                     MergedContextConfiguration mergedConfig) {
            // addFirst로 application.yml의 ${JWT_PUBLIC_KEY}보다 높은 우선순위로 주입
            context.getEnvironment().getPropertySources().addFirst(new MapPropertySource(
                    "jwtTestKeys",
                    Map.of("gateway.jwt.public-key", PUBLIC_KEY_BASE64)
            ));
        }

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
