package com.qtai.gateway.auth;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * RS256 JWT 검증용 JWK Set 노출.
 *
 * <p>application.yml의 {@code spring.security.oauth2.resourceserver.jwt.jwk-set-uri} 가
 * 본 엔드포인트를 가리키며, 각 서비스(BFF, AI)도 동일 URI로 키 캐시를 적재한다.
 *
 * <p>TODO(강태오): KeyPair 생성/로테이션을 K8s Secret + Vault 패턴으로 옮긴 뒤
 * 실제 공개키 모듈러스(n)와 지수(e)를 반환한다.
 */
@RestController
public class JwksController {

    @GetMapping(value = "/.well-known/jwks.json", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> jwks() {
        // 스캐폴딩: 더미 키. 실제로는 RSA Public Key의 n, e를 base64url 인코딩.
        return Map.of("keys", List.of(Map.of(
                "kty", "RSA",
                "use", "sig",
                "alg", "RS256",
                "kid", "qtai-stub-2026-05",
                "n", "STUB_MODULUS",
                "e", "AQAB"
        )));
    }
}
